
package org.tl.nettyServer.media.media.ts;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.media.acc.AACFrame;
import org.tl.nettyServer.media.media.acc.AACUtils;
import org.tl.nettyServer.media.media.flv.FLVUtils;
import org.tl.nettyServer.media.media.h264.H264CodecConfigParts;
import org.tl.nettyServer.media.media.h264.H264Utils;
import org.tl.nettyServer.media.media.mp3.MP3BufferedDecoder;
import org.tl.nettyServer.media.media.mp3.MP3HeaderData;
import org.tl.nettyServer.media.net.rtmp.codec.AudioCodec;
import org.tl.nettyServer.media.net.rtmp.codec.VideoCodec;
import org.tl.nettyServer.media.net.rtmp.event.AudioData;
import org.tl.nettyServer.media.net.rtmp.event.VideoData;
import org.tl.nettyServer.media.util.BufferUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.tl.nettyServer.media.media.ts.TransportStreamUtils_.*;


/**
 * FLV TO Mpeg2TS
 * @author pengliren
 * 	TS的分层结构
 *   	TS文件分为三层，如下所示（可以倒序看更好理解）：
 *   	◆ TS层（Transport Stream）：在PES层基础上加入了数据流识别信息和传输信息。一个ts视频文件含有多个ts单元包。
 * 	 	◆ PES层（Packet Elemental Stream）：在ES层基础上加入时间戳（PTS/DTS）等信息。每个ts单元含有一个pes头+多个es包。
 * 	 	◆ ES层（Elementary Stream）：压缩编码后的音视频数据。
 *
 *	TS层
 *		TS传输流，是由固定⻓度的包组成，含有独⽴时间基准的⼀个或多个节⽬，适⽤于误码较多的环境，并且从流的任意⼀段开始都可以独⽴解码。PS(Program Stream):节⽬流，PS流与TS流的区别在于，PS流的包结构是可变⻓度，⽽TS流的包结构是固定⻓度。
 * 		◆ ts包大小固定为188字节，ts层分为三个部分：ts header、adaptation field、payload。
 * 		◆ ts header固定4个字节；每188字节单元就有一个ts header。
 * 		◆ adaptation field可能存在也可能不存在，主要作用是给不足188字节的数据做填充。
 * 		◆ payload是 PES 数据，或者PAT，PMT等。PAT、PMT是解析ts寻找音视频流很重要的表。
 *
 *	TS Header
 * 		◆ TS Header格式如下：
 *			sync_byte                    // 8 bit
 *				同步字节（sync_byte）：固定的8bit字段，其值为“0100 0111”（0x47）。 应避免在为其他常规字段（例如PID）选择值时使用sync_byte的固定， 该字段是MPEG-2 TS传送包标识符。
 *     		transport_error_indicator    // 1 bit
 *     			传输误差指示符（transport_error_indicator）：长度为1bit， 设置为“1”时，表示相关传输流数据包中至少存在1个不可纠正的位错误。 该位可以由传输层外部的实体设置为“1”。 设置为“1”时，除非纠正了错误的位值，否则该位不应复位为“0”。
 *     		payload_unit_start_indicator // 1 bit
 *     			◆有效载荷单元起始符（payload_unit_start_indicator）：长度为1bit，对于携带PES包或PSI数据的传输流包具有规范含义。
 *				◆当TS包的有效载荷包含PES数据时，payload_unit_start_indicator具有以下含义：“1”表示该TS包的有效载荷从PES的第一个
 *					字节开始，“0”则表示PES包的起始地址并非有效载荷的开始。 如果payload_unit_start_indicator被设置为’1’，则在该TS包中有且仅有一个PES包。
 *				◆当TS包的有效载荷包含PSI数据时，payload_unit_start_indicator具有以下含义：如果TS包携带PSI部分的第一个字节，则
 *					payload_unit_start_indicator值应为’1’，表示TS包有效载荷的第一个字节包含pointer_field。
 *					如果TS包不携带PSI部分的第一个字节，则payload_unit_start_indicator值应为’0’，表示有效载荷中没有pointer_field。
 *				◆对于空包，payload_unit_start_indicator应设置为’0’。这意味着，该TS包只包含了MPEG2-TS规范未定义的用户自定义数据。
 *     		transport_priority           // 1 bit                 包头部分
 *     			◆传输优先级（transport_priority）：长度为1bit。 当设置为“1”时，它表示相关包（Packet）的优先级高于具有相同PID但没有将该位设置为“1”的其他包。
 *     				传输优先级机制可以再基本流中对数据进行优先级排序。
 *     		PID                          // 13 bit
 *     			PID：长度为13bit，指示存储在Packet有效载荷中的数据的类型。 PID值0x0000保留给程序关联表、0x0001保留用于条件访问表、0x0002 - 0x000F保留。
 *     				PID值0x1FFF保留用于空包。对应的PID表如下：
 *     				◆Value			Description
 * 					◆0x0000			Program Association Table
 * 					◆0x0001			Conditional Access Table
 * 					◆0x0002			Transport Stream Description Table
 * 					◆0x0003~0x000F	Reserved
 * 					◆0x0010~0x1FFE	May be assigned as network_PID, Program_map_PID, elementary_PID, or for other purposes
 * 					◆0x1FFF			Null Packet
 * 					注 - 允许PID值为0x0000,0x0001和0x0010-0x1FFE的传输包携带PCR
 *     		transport_scrambling_control // 2 bit
 *     			◆传输加扰控制（transport_scrambling_control）：长度为2bit，表示TS包有效载荷是否是加扰模式。 在加扰模式下，TS包头和可能存在的适配字段不应被加扰。 在空包的情况下，transport_scrambling_control字段的值应设为’00’。加扰表如下：
 *					Value	Description
 *					00	Not scrambled
 *					01	User-defined
 *					10	User-defined
 *					11	User-defined
 *     		adaptation_field_control     // 2 bit
 *     			◆自适应字段控制（adaptation_field_control）：长度为2bit，决定TS包头，后面是跟适配字段还是负载数据。
 *     				Value	Description
 * 					00	Reserved for future use by ISO/IEC
 * 					01	No adaptation_field, payload only
 * 					10	Adaptation_field only, no payload
 * 					11	Adaptation_field followed by payload
 *				当adaptation_field_control字段值为’00’是，解码器应该丢弃该Packet。 空包时，该字段的值应设为’01’。
 *     		continuity_counter           // 4 bit
 *				◆连续计数器（continuity_counter）：长度为4bit，连续计数器表示，有多少个TS包具有相同的PID。 当Packet的adaptation_field_control等于’00’或’10’时，continuity_counter不会递增。空包时，continuity_counter是未定义的。
 *			data_byte            // 8 bit
 *				◆数据字节（data_byte）：载荷部分的有效字节数据，数据字节应是来自PES包、PSI部分的数据的连续字节，
 *					PSI部分之后的包填充字节，或者不在这些结构中的私有数据。 在具有PID值0x1FFF的空包的情况下，
 *					可以为data_bytes分配任何值。 data_bytes的数量N由184减去adaptation_field（）中的字节数来指定。
 *
 *	Adaptation field
 *		在TS包中，打包后的PES数据，通常并不能满足188字节的长度，为了补足188字节，以及在系统层插入插入参考时钟（program clock reference， PCR），需要在TS包中插入长度可变的调整字段。
 *
 *  payload
 *  	前边提到了PAT和PMT，它们都是PSI之一（节目专用信息（Program Special Information， PSI）。
 *  	MPEG-2 TS传送的TS包携带两类信息：已压缩的音视频（PES）和与之相关的符号化表（PSI），由传送包PES的PID来标识。
 *  	如果是PSI，那么payload内容为PAT表结构、PMT表结构；如果为音视频，那么payload内容为PES包。
 *
 *  PES层
 *		PES包的第5个字节标识一整个PES包的长度，一般来说，一个PES包包含一帧图像，获取了PES的包长度Len，当接收到Len个字节后，将接收到的字节组成一个block，
 *		放入FIFO中，等待解码线程解码。DTS和PTS也在PES包中传送。
 *		            起始码流（packet_start_code_prefix）	 	24b		固定为00 00 01，借此可判断出PES包起始位置
 * 					流id（stream id）	 				 	8b		用来指示pes包的数据类型。表示音频通常取值0xc0，表示视频通常取值0xe0
 * 					包长度（PES_packet_length）				16b		用来指示接下来数据类型的大小，一般不会超过0xffff，除非包含的是视频数据
 *             		PES加扰控制（PES_scrambling_control）	 	2b		与TS包头加扰控制一致，00不加扰，其他值用户自定义
 *					PES优先级（PES_priority）	 				1b		置为1时代表比其他置为0的有效负载拥有更高的优先级
 * 					版权（copyright）	 					1b		1代表此有效负载被版权保护，0代表不确定
 *         			原始的或复制的（original_or_copy）	 		1b		1代表原始的，0代表复制的
 * 					显示解码时间戳标识符（PTS_DTS_flags）	 	2b		两个位分别代表是否有PTS（显示时间戳）或者DTS（解码时间戳）。此flag占据两个位，后面还有6个flag用来指示相应字段，这也是为什么上图7个flag占据8个位的原因。
 * 					PES头数据长度（PES_header_data_length）	8b		此指示符的数值代表的是填充字段和任选字段数值的大小
 * 					PTS DTS	33b	各33b，
 *					ES速率（ES Rate）						22b		由前面的flag指定是否存在，用来指定系统解码器解码PES包的速率，以50B/s为基本单位，这个速率会持续到新的ES Rate出现。
 * 					特技方式（trick_mode_control）			3b		由前面的相应flag指定是否存在。其中000快进，001慢动作，010冻结帧，011快速反向，100慢反向，其他值保留。
 * 					填充字节（stuffing_byte）					8b 		一般为11111111，由编码器插入，由解码器丢弃
 * 					覆盖字节（padding_byte）					8b		一般为11111111，由解码器丢弃。
 *	ES层介绍
 *		 Element Stream,即最基本的视音频数据了，在Ts封装格式中，视频使用H264压缩编码的，音频则是ACC压缩编码。
 *
 *		 h264视频：
 *       	现在我们需要知道要传输h264视频必须在前面加上一个NALU（network abstract layout uint）头部，这个NALU头部格式如下图所示：
 *			F（frobiden） 		1b	禁止位，发生语法错误时此为为1
 * 			NRI 				2b	参考级别，值越大，此NAL越重要
 * 			NAL_TYPE 			5b	指明了此NAL单元的类型，常见的取值如下图
 *
 *			nal_unit_type	说明
 * 						0	未使用
 * 						1	非IDR图像片，IDR指关键帧
 * 						2	片分区A
 * 						3	片分区B
 * 						4	片分区C
 * 						5	IDR图像片，即关键帧
 * 						6	补充增强信息单元(SEI)
 * 						7	SPS序列参数集
 * 						8	PPS图像参数集
 * 						9	分解符
 * 						10	序列结束
 * 						11	码流结束
 * 						12	填充
 * 						13~23	保留
 * 						24~31	未使用
 *       				牢记关键帧非关键帧，SEI，SPS，PPS的对应取值
 */
public class FLV2MPEGTSWriter_ {

	private static Logger log = LoggerFactory.getLogger(FLV2MPEGTSWriter.class);

	/* 转换的数据是从这流提供的，这里流是在SegmentFacade中注册的 */
	private IFLV2MPEGTSWriter writer;
	/* 视频pid */
	public final static int videoPID = 0x100; //二进制 1 0000 0000
	/* 音频pid */
	public final static int audioPID = 0x101; //	1 0000 0001
	/* 视频流id */
	private byte videoStreamID = (byte) 0xE0;
	/* 音频流id */
	private byte audioStreamID = (byte) 0xC0;
	/* 数据块大小 */
	private byte[] block = new byte[TS_PACKETLEN];
	/* 数据块大小 */
	protected long videoCCounter = -1L;

	protected long audioCCounter = -1L;

	protected long patCCounter = 0L;

	protected AACFrame aacFrame = null;

	protected H264CodecConfigParts h264CodecConfigPart = null;

	protected long lastAudioTimeCode = -1L;

	protected long lastVideoTimeCode = -1L;

	protected long lastPCRTimeCode = -1L;

	protected boolean isFirstAudioPacket = true;

	protected boolean isFirstVideoPacket = true;

	// fix rtmp mp3 to mpegts ts
	private int lastMP3SampleRate = -1;

	private long lastMP3TimeCode = -1;

	private byte[] mp3HeaderBuf;

	private MP3HeaderData mp3HeaderData;

	// fix rtmp aac to mpegts ts
	private int lastAACSampleRate = -1;

	private long lastAACTimeCode = -1L;

	protected WaitingAudio waitingAudio = new WaitingAudio();

	protected int pcrBufferTime = 750;

	protected int mpegtsAudioGroupCount = 3;

	protected int videoCodec = TransportStreamUtils.STREAM_TYPE_VIDEO_UNKNOWN;

	protected int audioCodec = TransportStreamUtils.STREAM_TYPE_AUDIO_UNKNOWN;

	public FLV2MPEGTSWriter_(IFLV2MPEGTSWriter writer, BufFacade videoConfig, BufFacade audioConfig) {
		
		this.writer = writer;
		if(videoConfig != null) {
			videoCodec = (byte) FLVUtils.getVideoCodec(videoConfig.getByte(0));
			if(videoCodec == VideoCodec.AVC.getId()) {
				videoConfig.readerIndex(0);
				h264CodecConfigPart = H264Utils.breakApartAVCC(videoConfig);
			}
		}
		
		if(audioConfig != null) {
			audioCodec = (byte)FLVUtils.getAudioCodec(audioConfig.getByte(0));
			if(audioCodec == AudioCodec.AAC.getId()) {
				audioConfig.readerIndex(2);
				aacFrame = AACUtils.decodeAACCodecConfig(audioConfig);
			}
		}
	}
	
	public void handleVideo(VideoData data) {
		
		BufFacade dataBuff = data.getData().asReadOnly();
		dataBuff.markReaderIndex();
		long ts90 = data.getTimestamp() * TIME_SCALE;
		lastVideoTimeCode = ts90;
		int dataLen = dataBuff.readableBytes();
		byte[] dataBytes = new byte[dataLen];
		dataBuff.readBytes(dataBytes);
		dataBuff.resetReaderIndex();
		//下面是解析flv视频数据
		/*
		H.264码流分Annex-B和AVCC两种格式。
			Annex-B：使用start code分隔NAL(start code为三字节或四字节，0x000001或0x00000001，一般是四字节)；SPS和PPS按流的方式写在头部（即在ES中）。

			AVCC：使用NALU长度（固定字节，通常为4字节）分隔NAL；在头部包含extradata(或sequence header)的结构体。（extradata包含分隔的字节数、SPS和PPS，具体结构见下）
				注意：SPS和PPS是封装在container中，每一个frame前面是这个frame的长度；SPS的头部是0x67，PPS的头部是0x68，要保持对数据的敏感性。
		*/
		//当前获取的还是flv数据。所以下面从dataBuff中读取的是flv的tag
		/**
		 * https://cloud.tencent.com/developer/article/1055557
		 *  Video Tag Data结构
		 *  	帧类型   	4bit
		 *  	编码ID   	4bit
		 *  	视频数据	    avcpacket:
		 *						AVCPacketType  8bit
		 *						CTS			   24bit
		 *						数据            AVCVIDEOPACKET:
		 *											长度      32bit nalu单元的长度，不包括长度字段。
		 *											nalu数据  NALU数据，没有四个字节的nalu单元头，直接从h264头开始，比如：65 ** ** **，41 **  ** **
		 *											....
		 *
		 * 	关于CTS：这是一个比较难以理解的概念，需要和pts，dts配合一起理解。
		 * 	首先，pts（presentation time stamps），dts(decoder timestamps)，cts(CompositionTime)的概念：
		 * 	pts：显示时间，也就是接收方在显示器显示这帧的时间。单位为1/90000 秒。
		 * 	dts：解码时间，也就是rtp包中传输的时间戳，表明解码的顺序。单位单位为1/90000 秒。——根据后面的理解，pts就是标准中的CompositionTime
		 * 	cts偏移：cts = (pts - dts) / 90 。cts的单位是毫秒。
		 * 	pts和dts的时间不一样，应该只出现在含有B帧的情况下，也就是profile main以上。baseline是没有这个问题的，
		 * 	baseline的pts和dts一直想吐，所以cts一直为0。在flv tag中的时戳就是DTS。
		 *  研究 一下文档，ISO/IEC 14496-12:2005(E)  8.15 Time to Sample Boxes，
		 *  发现CompositionTime就是presentation time stamps，只是叫法不同。——需要再进一步确认。在上图中，cp就是pts，显示时间。
		 *  DT是解码时间，rtp的时戳。I1是第一个帧，B2是第二个，后面的序号就是摄像头输出的顺序。决定了显示的顺序。DT，是编码的顺序，
		 *  特别是在有B帧的情况，P4要在第二个解，因为B2和B3依赖于P4，但是P4的显示要在B3之后，因为他的顺序靠后。
		 *  这样就存在显示时间CT(PTS)和解码时间DT的差，就有了CT偏移。P4解码时间是10，但是显示时间是40，
		 *
		 */
		if (dataBuff.readableBytes() >= 2) {
			int firstByte = dataBuff.readByte();
			//第二字节AVCPacketType
			int secondByte = dataBuff.readByte();
			//第1个字节的前4位的数值表示帧类型(keyframe...) 后4位的数值表示视频编码类型(JPEG...)
			int codec = FLVUtils.getVideoCodec(firstByte);
			//Type of video frame. The following values are defined:
			//1 = key frame (for AVC, a seekable frame)
			//2 = inter frame (for AVC, a non-seekable frame)
			//3 = disposable inter frame (H.263 only)
			//4 = generated key frame (reserved for server use only)
			//5 = video info/command frame
			int frameType = FLVUtils.getFrameType(firstByte);
			int paloadLen = 0;
			int naluNum = 0;
			videoCodec = codec;
			// AVCPacketType
			// 0 = AVC sequence header （AVCDecoderConfigurationRecord）
			// 1 = AVC NALU
			// 2 = AVC end of sequence (lower level NALU sequence ender is not required or supported)
			//果视频的格式是AVC（H.264）的话，VideoTagHeader会多出4个字节的信息 AVCPacketType (1b)和CompositionTime(3b)
			if ((codec == VideoCodec.AVC.getId()) && (secondByte != 1)) {
				if (secondByte == 0) {
					//从新开始读取数据
					dataBuff.readerIndex(0);
					if (h264CodecConfigPart == null) {
						h264CodecConfigPart = H264Utils.breakApartAVCC(dataBuff);
						dataBuff.readerIndex(5);
					}
				}
			} else if (codec == VideoCodec.AVC.getId()) {
				//解析avc格式数据的编码 H264比特流 = Start_Code_Prefix + NALU + Start_Code_Prefix + NALU + …
				//此处获取视频数据的第cts时间
				int cts = BufferUtil.byteArrayToInt(dataBytes, 2, 3) * TIME_SCALE;
				//显示时间 = 解码时间 + 偏移时间
				long ts = ts90 + cts;
				//数据包的类型，如果是有消息头的就要从10开始是有效的数据，否则从5开始是有效数据
				int ptdDtsFlag = 1;
				int loop = 5;
				int naluLen;
				// codec（1b）+AVCPacketType(1b) + CompositionTime(3b)
				dataBuff.readerIndex(5);
				List<TSPacketFragment> tsPacketList = new ArrayList<TSPacketFragment>();
				byte[] h264Startcode = new byte[4]; // h264 start code 0x00 0x00 0x00 0x01
				h264Startcode[3] = 1; // 最后一个字节接收1 则为 0x01 构成 startCode
				int sps = 0;
				int pps = 0;
				int pd = 0;
				//下面是VIDEODATA数据
				//每个NALU包前面都有（lengthSizeMinusOne & 3）+1个字节的NAL包长度描述
				while (loop + 4 <= dataLen) {
					//前4位是NALU length
					naluLen = BufferUtil.byteArrayToInt(dataBytes, loop, 4);
					loop += 4;

					if ((naluLen <= 0) || (loop + naluLen > dataLen))break;
					//nalu每个NALU第一个字节的前5位标明的是该NAL包的类型，即NAL nal_unit_type
					/*
				  		* 	NALU_TYPE_SLICE 1
						* 	NALU_TYPE_DPA 2
						* 	NALU_TYPE_DPB 3
						* 	NALU_TYPE_DPC 4
						* 	NALU_TYPE_IDR 5
						* 	NALU_TYPE_SEI 6
						* 	NALU_TYPE_SPS 7
						* 	NALU_TYPE_PPS 8
						* 	NALU_TYPE_AUD 9//访问分隔符
						* 	NALU_TYPE_EOSEQ 10
						* 	NALU_TYPE_EOSTREAM 11
						* 	NALU_TYPE_FILL 12
					 */

					int naluType = dataBytes[loop] & 0x1F;
					//SPS即Sequence Paramater Set，又称作序列参数集。SPS中保存了一组编码视频序列(Coded video sequence)的全局参数
					//所谓的编码视频序列即原始视频的一帧一帧的像素数据经过编码之后的结构组成的序列。
					// 在H.264标准协议中规定了多种不同的NAL Unit类型，其中类型7表示该NAL Unit内保存的数据为Sequence Paramater Set
					if (naluType == 7) { // sps
						sps = 1;
					} else if (naluType == 8) { // pps
						//为图像参数集Picture Paramater Set(PPS)
						pps = 1;
					} else if (naluType == 9) { // pd
						pd = 1;
					}

					tsPacketList.add(new TSPacketFragment(h264Startcode, 0, h264Startcode.length));
					tsPacketList.add(new TSPacketFragment(dataBytes, loop, naluLen));
					paloadLen += (naluLen + h264Startcode.length);
					naluNum++;
					loop += naluLen;

					if (loop >= dataLen) break;
				}
				//两种打包方法。一种即Annex B格式，另一种称为AVCC格式
				//打包 es 层数据时 pes 头和 es 数据之间要加入一个 aud(type=9) 的 nalu
				int idx = 0;
				if (pd == 0) {
					byte[] annex = new byte[6];
					annex[3] = 0x01; // 0x00 0x00 0x00 0x01
					annex[4] = 0x09; // naluType
					if (frameType == 1) //key frame (for AVC, a seekable frame)
						annex[5] = 16;
					else if (frameType == 3) //disposable inter frame (H.263 only)
						annex[5] = 80;
					else
						annex[5] = 48;
					tsPacketList.add(idx, new TSPacketFragment(annex, 0, annex.length));
					idx++;
					paloadLen += annex.length;
					naluNum++;
					naluLen = 1;
				} else {
					idx = 2;
				}

				// sps and pps
				// frameType == 1 : key frame (for AVC, a seekable frame)
				if (frameType == 1 && (pps == 0 || pd == 0)) {
					if (frameType == 1 && sps == 0 && h264CodecConfigPart != null && h264CodecConfigPart.getSps() != null) {
						byte[] h264SpsStartcode = new byte[4];
						h264SpsStartcode[3] = 1;
						tsPacketList.add(idx, new TSPacketFragment(h264SpsStartcode, 0, h264SpsStartcode.length));
						idx++;
						tsPacketList.add(idx, new TSPacketFragment(h264CodecConfigPart.getSps(), 0, h264CodecConfigPart.getSps().length));
						idx++;
						paloadLen += (h264CodecConfigPart.getSps().length + h264SpsStartcode.length);
						naluNum++;
						naluLen = 1;
					}

					if (frameType == 1 && pps == 0 && h264CodecConfigPart != null && h264CodecConfigPart.getPpss() != null) {
						List<byte[]> ppss = h264CodecConfigPart.getPpss();
						for (byte[] b : ppss) {
							byte[] h264PpsStartcode = new byte[4]; 
							h264PpsStartcode[3] = 1;
							tsPacketList.add(idx, new TSPacketFragment(h264PpsStartcode, 0, h264PpsStartcode.length));
							idx++;
							tsPacketList.add(idx, new TSPacketFragment(b, 0, b.length));
							idx++;
							paloadLen += (b.length + h264PpsStartcode.length);
							naluNum++;
							naluLen = 1;
						}
					}
				}
				//将解析出来的数据进行ts打包
				//ts header   adaptation field   payload。
				if (naluNum > 0) {
					TSPacketFragment tsPacket =  tsPacketList.remove(0);
					int offset = tsPacket.getOffset();
					int len = tsPacket.getLen();
					byte[] buff = tsPacket.getBuffer();
					long pcr = getPCRTimeCode();
					int paloadReadedLen = 0;
					/*
					 *	payload_unit_start_indicator // 1 bit
		 			 * 	◆有效载荷单元起始符（payload_unit_start_indicator）：长度为1bit，对于携带PES包或PSI数据的传输流包具有规范含义。
		 			 *	◆当TS包的有效载荷包含PES数据时，payload_unit_start_indicator具有以下含义：“1”表示该TS包的有效载荷从PES的第一个
		 			 *		字节开始，“0”则表示PES包的起始地址并非有效载荷的开始。 如果payload_unit_start_indicator被设置为’1’，则在该TS包中有且仅有一个PES包。
		 			 *	◆当TS包的有效载荷包含PSI数据时，payload_unit_start_indicator具有以下含义：如果TS包携带PSI部分的第一个字节，则
		 			 *		payload_unit_start_indicator值应为’1’，表示TS包有效载荷的第一个字节包含pointer_field。
		 			 *		如果TS包不携带PSI部分的第一个字节，则payload_unit_start_indicator值应为’0’，表示有效载荷中没有pointer_field。
		 			 *	◆对于空包，payload_unit_start_indicator应设置为’0’。这意味着，该TS包只包含了MPEG2-TS规范未定义的用户自定义数据。
					 *
					 */
					int stt = 1; // pay_load_unit_start_indicator
					int pesPayloadWritten = 0;
					while (true) {
						int unReadPayloadLen = paloadLen - paloadReadedLen;
						stt = 1;
						/**
						 * adaption_field_control
						 * 00：保留；
						 * 01：表示无调整字段，只有有效负载数据；
						 * 10：表示只有调整字段，无有效负载数据；
						 * 11：表示有调整字段，且其后跟随有效负载数据；
						 */
						int atf = 1; // adaption_field_control
						int readPayloadLen = 0;
						if (unReadPayloadLen > 32725) unReadPayloadLen = 32725; //maxPesDataLen is 32725

						while (true) {
							int tsIdx = 0;

							// ts header 4 byte
							block[tsIdx] = SYNCBYTE; // ts header start sync_byte 
							tsIdx++;
							/**
							 * 	transport_error_indicator     			:    1;      //传输错误标志位，一般传输错误的话就不会处理这个包了
							 *  payload_unit_start_indicator  			:    1;      //有效负载的开始标志，根据后面有效负载的内容不同功能也不同
							 *  transport_priority            			:    1;      //传输优先级位，1表示高优先级
							 *  PID                           			:    13;     //有效负载数据的类型
							 *  transport_scrambling_control   			:    2;      //加密标志位,00表示未加密
							 *  adaption_field_control        			:    2;      //调整字段控制,。01仅含有效负载，10仅含调整字段，11含有调整字段和有效负载。为00的话解码器不进行处理。
							    continuity_counter             			:    4;   	 //一个4bit的计数器，范围0-15
							*/
							//此处构建上面4个参数，第一个字节取前三位和 videPID的后前5位 (0x1F = 1 1111)
							//“1”表示该TS包的有效载荷从PES的第一个字节开始
							// stt!=0 则64的二进制（100 0000）即 010
							// transport_error_indicator=0,
							// payload_unit_start_indicator=1,
							// transport_priority=0
							block[tsIdx] = (byte) ((stt != 0 ? 64 : 0) + (0x1F & videoPID >> 8));
							tsIdx++;
							//此处取videoPID得后8位 （0xFF=1111 1111）
							block[tsIdx] = (byte) (videoPID & 0xFF);
							tsIdx++;
							if (videoCCounter == -1L)
								videoCCounter = 1L;
							else
								videoCCounter += 1L;
							/* 0xf 对应二进制00001111  videoCCounter & 0xf 保留这个数的低4位 强制转换成一个byte，则会把前面全部截掉，保留后8位*/
							//加一个16=（二进制）01 0000目的是 adaption_field_control  = 01  transport_scrambling_control=0
							block[tsIdx] = (byte) (int) (16L + (videoCCounter & 0xF)); // ts header end

							// 以上共4 byte
							tsIdx++;
							int pesHeaderLen = 0;
							//  https://www.jianshu.com/p/64463c3905d3 此处说明pes-header长度
							//pes包,前面固定有9byte,后面根据参数变动
							if (stt != 0)
								pesHeaderLen = 9 + (ptdDtsFlag != 0 ? 10 : 5);
							// ts数据包的总大小 - 已经装配的头 - 消息头数据- pesheaderlength
							int tsPaloadLen = TS_PACKETLEN - tsIdx - pesHeaderLen;
							if (tsPaloadLen > unReadPayloadLen - readPayloadLen)
								tsPaloadLen = unReadPayloadLen - readPayloadLen;
							int atfLen;
							int fillNullLen;
							long tempts;
							/**
							 * adaption_field_control
							 * 00：保留；
							 * 01：表示无调整字段，只有有效负载数据；
							 * 10：表示只有调整字段，无有效负载数据；
							 * 11：表示有调整字段，且其后跟随有效负载数据；
							 */
							if (atf != 0) {
								int thirdByte = 3;
																		//0010 0000
								block[thirdByte] = (byte) (block[thirdByte] | 0x20); //由 01 -> 11
								//自适应区长度固定8字节
								atfLen = 8;
								tsPaloadLen = TS_PACKETLEN - tsIdx - pesHeaderLen - atfLen;
								if (tsPaloadLen > unReadPayloadLen - readPayloadLen)
									tsPaloadLen = unReadPayloadLen - readPayloadLen;
								fillNullLen = 0;
								if (tsIdx + tsPaloadLen + pesHeaderLen + atfLen < TS_PACKETLEN)
									fillNullLen = TS_PACKETLEN - (tsIdx + tsPaloadLen + pesHeaderLen + atfLen);
								//adaptation_field_length
								block[tsIdx] = (byte) (atfLen - 1 + fillNullLen & 0xFF);
								tsIdx++;
								//discontinuity_indicator
								block[tsIdx] = (byte) ((isFirstVideoPacket ? 0x80 : 16) | (frameType == 1 ? 64 : 0));
								tsIdx++;
								tempts = pcr;
								tempts <<= 7;
								byte[] pcrData = BufferUtil.longToByteArray(tempts);
								block[tsIdx + 4] = (byte) ((pcrData[7] & 0x80) + 126);
								block[tsIdx + 3] = (byte) (pcrData[6] & 0xFF);
								block[tsIdx + 2] = (byte) (pcrData[5] & 0xFF);
								block[tsIdx + 1] = (byte) (pcrData[4] & 0xFF);
								block[tsIdx] = (byte) (pcrData[3] & 0xFF);
								tsIdx += 6;
								if (fillNullLen > 0) {
									System.arraycopy(TransportStreamUtils.FILL, 0, block, tsIdx, fillNullLen);
									tsIdx += fillNullLen;
								}
								tsPaloadLen = TS_PACKETLEN - tsIdx - pesHeaderLen;
								if (tsPaloadLen > unReadPayloadLen - readPayloadLen)
									tsPaloadLen = unReadPayloadLen - readPayloadLen;
								atf = 0;
							} else if (tsIdx + tsPaloadLen + pesHeaderLen < TS_PACKETLEN) {
								atfLen = TS_PACKETLEN - (tsIdx + tsPaloadLen + pesHeaderLen);
								int third = 3;
								block[third] = (byte) (block[third] | 0x20);
								if (atfLen > 1) {
									atfLen--;
									block[tsIdx] = (byte) (atfLen & 0xFF);
									tsIdx++;
									block[tsIdx] = 0;
									tsIdx++;
									atfLen--;
									if (atfLen > 0)
										System.arraycopy(TransportStreamUtils.FILL, 0, block, tsIdx, atfLen);
									tsIdx += atfLen;
								} else {
									block[tsIdx] = 0;
									tsIdx++;
								}
							}
							if (stt != 0) { // pay_load_unit_start_indicator = 1
								//pesheader 进行组织
								block[tsIdx] = 0;
								tsIdx++;
								block[tsIdx] = 0;
								tsIdx++;
								block[tsIdx] = 1;
								tsIdx++;
								block[tsIdx] = videoStreamID;
								tsIdx++;
								atfLen = ptdDtsFlag != 0 ? 10 : 5;
								fillNullLen = unReadPayloadLen + atfLen + 3;
								if (fillNullLen >= 65536)
									log.warn("toolong: {}", fillNullLen);
								BufferUtil.intToByteArray(fillNullLen, block, tsIdx, 2);
								tsIdx += 2;
								block[tsIdx] = (byte) 0x84;
								tsIdx++;
								block[tsIdx] = (byte) (ptdDtsFlag != 0 ? 0xC0 : 0x80);
								tsIdx++;
								block[tsIdx] = (byte) atfLen;
								tsIdx++;
								tempts = ts;
								block[tsIdx + 4] = (byte) (int) (((tempts & 0x7F) << 1) + 1L);
								tempts >>= 7;
								block[tsIdx + 3] = (byte) (int) (tempts & 0xFF);
								tempts >>= 8;
								block[tsIdx + 2] = (byte) (int) (((tempts & 0x7F) << 1) + 1L);
								tempts >>= 7;
								block[tsIdx + 1] = (byte) (int) (tempts & 0xFF);
								tempts >>= 8;
								block[tsIdx] = (byte) (int) (((tempts & 0x7) << 1) + 1L + (ptdDtsFlag != 0 ? 48 : 32));
								tsIdx += 5;
								if (ptdDtsFlag != 0) {
									tempts = ts90;
									block[tsIdx + 4] = (byte) (int) (((tempts & 0x7F) << 1) + 1L);
									tempts >>= 7;
									block[tsIdx + 3] = (byte) (int) (tempts & 0xFF);
									tempts >>= 8;
									block[tsIdx + 2] = (byte) (int) (((tempts & 0x7F) << 1) + 1L);
									tempts >>= 7;
									block[tsIdx + 1] = (byte) (int) (tempts & 0xFF);
									tempts >>= 8;
									block[tsIdx] = (byte) (int) (((tempts & 0x7) << 1) + 1L + (ptdDtsFlag != 0 ? 16 : 32));
									tsIdx += 5;
								}
							}

							while (true) {
								atfLen = tsPaloadLen;
								if (atfLen > len - pesPayloadWritten)
									atfLen = len - pesPayloadWritten;
								System.arraycopy(buff, offset + pesPayloadWritten, block, tsIdx, atfLen);
								pesPayloadWritten += atfLen;
								tsIdx += atfLen;
								paloadReadedLen += atfLen;
								tsPaloadLen -= atfLen;
								readPayloadLen += atfLen;
								if (pesPayloadWritten >= len) {
									pesPayloadWritten = 0;
									if (tsPacketList.size() > 0) {
										tsPacket = tsPacketList.remove(0);
										offset = tsPacket.getOffset();
										len = tsPacket.getLen();
										buff = tsPacket.getBuffer();
									}
								}
								if (tsIdx >= TS_PACKETLEN || readPayloadLen >= unReadPayloadLen || paloadReadedLen >= paloadLen) break;
							}
							stt = 0;
							writer.nextBlock(ts90, block);
							isFirstVideoPacket = false;
							if (readPayloadLen >= unReadPayloadLen || paloadReadedLen >= paloadLen) break;
						}
						if (paloadReadedLen >= paloadLen) break;
					}
				}
			} else {
				log.debug("video data is not h264/avc!");
			}
		}
	}
	
	public void handleAudio(AudioData data) {
		
		BufFacade dataBuff = data.getData().asReadOnly();
		dataBuff.markReaderIndex();
		int dataLen = dataBuff.readableBytes();
		byte[] dataBytes = new byte[dataLen];
		dataBuff.readBytes(dataBytes);
		dataBuff.resetReaderIndex();
		byte firstByte = dataBuff.readByte();
		byte secondByte = dataBuff.readByte();
		int codecId = FLVUtils.getAudioCodec(firstByte);
		audioCodec = codecId;
		if (codecId == AudioCodec.AAC.getId() && secondByte != 1) {
			if (secondByte == 0) {
				AACFrame tempFrame = AACUtils.decodeAACCodecConfig(dataBuff);
				if (aacFrame == null && tempFrame != null) {
					aacFrame = tempFrame;
				}
				if(tempFrame == null) log.error("audio error configure:{}", dataBuff);
			}
		} else if ((codecId == AudioCodec.AAC.getId() || codecId == AudioCodec.MP3.getId()) 
				&& (codecId != AudioCodec.AAC.getId() || aacFrame != null)) {
			long ts = data.getTimestamp() * TIME_SCALE;
			long incTs;
			long fixTs;
			int interval = -1;
			// fix low-resolution timestamp in RTMP to MPEG-TS
			if(codecId == AudioCodec.AAC.getId()) {
				 
				if(lastAACSampleRate == -1 || lastAACSampleRate != aacFrame.getSampleRate()) {
					lastAACSampleRate = this.aacFrame.getSampleRate();
	                lastAACTimeCode = Math.round(data.getTimestamp() * lastAACSampleRate / 1000.0D);
				} else {
					incTs = lastAACTimeCode + aacFrame.getSampleCount();
	                fixTs = Math.round(incTs * 1000.0D / lastAACSampleRate);
	                interval = (int)Math.abs(fixTs - data.getTimestamp());
					if (interval <= 1) {
						ts = Math.round(incTs * 90000L / lastAACSampleRate);
						lastAACTimeCode = incTs;
					} else {
						lastAACTimeCode = Math.round(data.getTimestamp() * lastAACSampleRate / 1000.0D);
					}
				}
				
				// aacFram size = 7 byte(adts header) + aac data size - 2 byte(0xAF 0x00)
				aacFrame.setSize(7 + dataBytes.length - 2);
				byte[] adts = new byte[7];
				AACUtils.frameToADTSBuffer(aacFrame, adts, 0);
				
				waitingAudio.fragments.add(new TSPacketFragment(adts, 0, adts.length));
				waitingAudio.size += adts.length;
				waitingAudio.fragments.add(new TSPacketFragment(dataBytes, 2, dataBytes.length - 2));
				waitingAudio.size += dataBytes.length - 2;
				waitingAudio.codec = codecId;				
			} else if(codecId == AudioCodec.MP3.getId()) {
				
				try {
					if (mp3HeaderBuf == null) {
						mp3HeaderBuf = new byte[4];
						mp3HeaderData = new MP3HeaderData();
					}
					System.arraycopy(dataBytes, 1, mp3HeaderBuf, 0, 4);
					int syncData = MP3BufferedDecoder.syncHeader((byte)0, mp3HeaderBuf, mp3HeaderData);
					if(syncData != 0) {
						MP3BufferedDecoder.decodeHeader(syncData, 0, mp3HeaderData);
						int sampleCount = MP3BufferedDecoder.samples_per_frame(this.mp3HeaderData);
		                int sampleRate = MP3BufferedDecoder.frequency(mp3HeaderData);
		                
		                if (lastMP3SampleRate == -1 || lastMP3SampleRate != sampleRate) {
							lastMP3SampleRate = sampleRate;
							lastMP3TimeCode = Math.round(data.getTimestamp() * lastMP3SampleRate / 1000.0D);
						} else {
							incTs = lastMP3TimeCode + sampleCount;
							fixTs = Math.round(incTs * 1000.0D / lastMP3SampleRate);
							interval = (int) Math.abs(fixTs - data.getTimestamp());
							if (interval <= 1) {
								//long ts = Math.round(data.getTimestamp() * (lastMP3SampleRate / 1000));
								ts = Math.round(incTs * 90000L / lastMP3SampleRate);
								lastMP3TimeCode = incTs;
							} else {
								lastMP3TimeCode = Math.round(data.getTimestamp() * lastMP3SampleRate / 1000.0D);
							}
						}
					}					
				} catch (Exception e) {
					log.debug("mp3 header parse fail: {}", e.toString());
				}
				
				waitingAudio.fragments.add(new TSPacketFragment(dataBytes, 1, dataBytes.length - 1));
	            waitingAudio.size += dataBytes.length - 1;
	            waitingAudio.codec = codecId;
			}
			
			waitingAudio.count += 1;
			if (waitingAudio.timecode == -1L)
				waitingAudio.timecode = ts;
			waitingAudio.lastTimeCode = ts;
			if (waitingAudio.count >= mpegtsAudioGroupCount) {
				lastAudioTimeCode = waitingAudio.timecode;
				writeAudioPackets(waitingAudio);
				waitingAudio.clear();
			}
		}
	}
	
	/**
	 *  write mult audio packets
	 * @param waitingAudio
	 * @throws IOException 
	 */
	private void writeAudioPackets(WaitingAudio waitingAudio) {
		
		int size = waitingAudio.size;
		long ts = waitingAudio.timecode;
		long ts90 = waitingAudio.timecode;
		int ptdDtsFlag = 0;
		int totalWritten = 0;
		TSPacketFragment packetFragment = (TSPacketFragment) waitingAudio.fragments.remove(0);
		int writtenLen = 0;
		int offset = packetFragment.getOffset();
		int len = packetFragment.getLen();
		byte[] data = packetFragment.getBuffer();
		int stt = 1; // Payload unit start indicator.
		while (true) {

			int pos = 0;
			block[pos] = SYNCBYTE;
			pos++;
			block[pos] = (byte) ((stt != 0 ? 64 : 0) + (0x1F & audioPID >> 8));
			pos++;
			block[pos] = (byte) (audioPID & 0xFF);
			pos++;
			if (audioCCounter == -1L)
				audioCCounter = 1L;
			else
				audioCCounter += 1L;
			block[pos] = (byte) (int) (16L + (audioCCounter & 0xF));
			pos++;
			int pesHeaderLen = 0;
			if (stt != 0) pesHeaderLen = 9 + (ptdDtsFlag != 0 ? 10 : 5); // pes header len
			int count = TS_PACKETLEN - pos - pesHeaderLen;
			if (count > size - totalWritten) count = size - totalWritten;
			int total = 0;
			int pesLen;
			if (pos + count + pesHeaderLen < TS_PACKETLEN) { // 当数据不够188个字节时 需要用自适应区填满
				total = TS_PACKETLEN - (pos + count + pesHeaderLen);
				int thirdByte = 3;
				block[thirdByte] = (byte) (block[thirdByte] | 0x20);
				if (total > 1) {
					total--;
					block[pos] = (byte) (total & 0xFF);
					pos++;
					block[pos] = 0;
					pos++;
					total--;
					if (total > 0) System.arraycopy(TransportStreamUtils.FILL, 0, block, pos, total);
					pos += total;
				} else {
					block[pos] = 0;
					pos++;
				}
			}
			if (stt != 0) {
				// packet_start_code_prefix 0x000001
				block[pos] = 0x00;
				pos++;
				block[pos] = 0x00;
				pos++;
				block[pos] = 0x01;
				pos++;
				block[pos] = audioStreamID; // stream_id
				pos++;
				total = ptdDtsFlag != 0 ? 10 : 5;
				pesLen = size + total + 3;
				BufferUtil.intToByteArray(pesLen, block, pos, 2); // PES_packet_length
				pos += 2;
				block[pos] = (byte)0x80;
				pos++;
				block[pos] = (byte) (ptdDtsFlag != 0 ? 0xC0 : 0x80);
				pos++;
				block[pos] = (byte) total;
				pos++;
				block[pos + 4] = (byte) (int) (((ts & 0x7F) << 1) + 1L);
				ts >>= 7;
				block[pos + 3] = (byte) (int) (ts & 0xFF);
				ts >>= 8;
				block[pos + 2] = (byte) (int) (((ts & 0x7F) << 1) + 1L);
				ts >>= 7;
				block[pos + 1] = (byte) (int) (ts & 0xFF);
				ts >>= 8;
				block[pos] = (byte) (int) (((ts & 0x7) << 1) + 1L + (ptdDtsFlag != 0 ? 48 : 32));
				pos += 5;
				if (ptdDtsFlag != 0) {
					block[pos + 4] = (byte) (int) (((ts & 0x7F) << 1) + 1L);
					ts >>= 7;
					block[pos + 3] = (byte) (int) (ts & 0xFF);
					ts >>= 8;
					block[pos + 2] = (byte) (int) (((ts & 0x7F) << 1) + 1L);
					ts >>= 7;
					block[pos + 1] = (byte) (int) (ts & 0xFF);
					ts >>= 8;
					block[pos] = (byte) (int) (((ts & 0x7) << 1) + 1L + 32L);
					pos += 5;
				}
			}
			while (true) {
				total = count;
				if (total > len - writtenLen)
					total = len - writtenLen;
				System.arraycopy(data, offset + writtenLen, block, pos, total);
				writtenLen += total;
				pos += total;
				totalWritten += total;
				count -= total;
				if (writtenLen >= len) {
					writtenLen = 0;
					if (waitingAudio.fragments.size() > 0) {
						packetFragment = (TSPacketFragment) waitingAudio.fragments.remove(0);
						offset = packetFragment.getOffset();
						len = packetFragment.getLen();
						data = packetFragment.getBuffer();
					}
				}
				if (pos >= TS_PACKETLEN || totalWritten >= size) break;
			}
			stt = 0;
			writer.nextBlock(ts90, block);
			if (totalWritten >= size) break;
		}
	}
	
	/**
	 * add pat pmt
	 * @return
	 */
	public void addPAT(long ts) {		
		fillPAT(block, 0, patCCounter);
		writer.nextBlock(ts, block);
		fillPMT(block, 0, patCCounter, videoPID, audioPID, videoCodecToStreamType(videoCodec), audioCodecToStreamType(audioCodec));
		writer.nextBlock(ts, block);
		this.patCCounter++;
	}
	
	private long getPCRTimeCode() {
		long ts = -1L;
		if ((lastAudioTimeCode >= 0L) && (lastVideoTimeCode >= 0L))
			ts = Math.min(lastAudioTimeCode, lastVideoTimeCode);
		else if (lastAudioTimeCode >= 0L)
			ts = lastAudioTimeCode;
		else if (lastVideoTimeCode >= 0L)
			ts = lastVideoTimeCode;
		if ((lastPCRTimeCode != -1L) && (ts < lastPCRTimeCode))
			ts = lastPCRTimeCode;
		if (ts < 0L)
			ts = 0L;
		if (ts >= pcrBufferTime)
		      ts -= pcrBufferTime;
		lastPCRTimeCode = ts;
		return ts;
	}
	
	public long getLastPCRTimeCode() {
		return lastPCRTimeCode;
	}

	public void setLastPCRTimeCode(long lastPCRTimeCode) {
		this.lastPCRTimeCode = lastPCRTimeCode;
	}

	public long getVideoCCounter() {
		return videoCCounter;
	}

	public int getVideoCodec() {
		return videoCodec;
	}

	public int getAudioCodec() {
		return audioCodec;
	}



	/**
	 * 
	 * @author pengliren
	 *
	 */
	class WaitingAudio {

		long timecode = -1L;
		long lastTimeCode = -1L;
		int count = 0;
		int size = 0;
		int codec = 0;
		List<TSPacketFragment> fragments = new ArrayList<TSPacketFragment>();

		WaitingAudio() {
		}

		public void clear() {
			timecode = -1L;
			lastTimeCode = -1L;
			count = 0;
			size = 0;
			fragments.clear();
		}

		public boolean isEmpty() {
			return size == 0;
		}

		public int size() {
			return size;
		}
	}
}
