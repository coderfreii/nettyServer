package org.tl.nettyServer.media.media.acc;

import io.netty.buffer.Unpooled;
import org.tl.nettyServer.media.buf.BufFacade;

/**
 * AAC Utils
 *
 * @author pengliren
 */
public class AACUtils {

    public static final int AAC_HEADER_SIZE = 7;
    public static final int[] AAC_SAMPLERATES = {96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350};
    public static final int[] AAC_CHANNELS = {0, 1, 2, 3, 4, 5, 6, 8};

    public static String profileObjectTypeToString(int profileType) {
        String str = "Unknown[" + profileType + "]";
        switch (profileType) {
            case 0:
                str = "NULL[0]";
                break;
            case 1:
                str = "Main";
                break;
            case 2:
                str = "LC";
                break;
            case 3:
                str = "SBR";
                break;
            case 4:
                str = "LongTermPrediction";
                break;
            case 5:
                str = "HE";
                break;
            case 6:
                str = "Scalable";
                break;
            case 7:
                str = "TwinVQ";
                break;
            case 8:
                str = "CELP";
                break;
            case 9:
                str = "HVXC";
                break;
            case 10:
                str = "Reserved[10]";
                break;
            case 11:
                str = "Reserved[11]";
                break;
            case 12:
                str = "TTSI";
                break;
            case 13:
                str = "Synthetic";
                break;
            case 14:
                str = "WavetableSynthesis";
                break;
            case 15:
                str = "GeneralMIDI";
                break;
            case 16:
                str = "AlgorithmicSynthesisAndAudioFX";
                break;
            case 17:
                str = "LowComplexityWithErrorRecovery";
                break;
            case 18:
                str = "Reserved[18]";
                break;
            case 19:
                str = "LongTermPredictionWithErrorRecover";
                break;
            case 20:
                str = "ScalableWithErrorRecovery";
                break;
            case 21:
                str = "TwinVQWithErrorRecovery";
                break;
            case 22:
                str = "BSACWithErrorRecovery";
                break;
            case 23:
                str = "LDWithErrorRecovery";
                break;
            case 24:
                str = "CELPWithErrorRecovery";
                break;
            case 25:
                str = "HXVCWithErrorRecovery";
                break;
            case 26:
                str = "HILNWithErrorRecovery";
                break;
            case 27:
                str = "ParametricWithErrorRecovery";
                break;
            case 28:
                str = "Reserved[28]";
                break;
            case 29:
                str = "Reserved[29]";
                break;
            case 30:
                str = "Reserved[30]";
                break;
            case 31:
                str = "Reserved[31]";
        }
        return str;
    }

    public static int sampleRateToIndex(int rate) {
        int rateIdx = 0;
        for (int i = 0; i < AAC_SAMPLERATES.length; i++) {
            if (rate != AAC_SAMPLERATES[i]) continue;
            rateIdx = i;
            break;
        }
        return rateIdx;
    }

    public static AACFrame decodeFrame(BufFacade data) {

        if (data.readableBytes() < 7) return null;
        AACFrame aacFrame = null;
        byte first = data.readByte();
        byte second = data.readByte();
        byte third = data.readByte();
        if ((first & 0xFF) == 0xFF && (second & 0xF0) == 0xF0) {
            int rate = 0;
            int channels = 0;
            boolean errorBitsAbsent = (second & 0x1) == 1;
            int profileType = (third >> 6 & 0x3) + 1;
            int rateIdx = third & 0x3C;
            rateIdx >>= 2;
            if (rateIdx >= 0 && rateIdx < AAC_SAMPLERATES.length) {
                rate = AAC_SAMPLERATES[rateIdx];
                int channelIdx = third & 0x1;
                channelIdx <<= 2;
                byte fourth = data.readByte();
                channelIdx += ((byte) (fourth >> 6) & 0x3);
                if (channelIdx >= 0 && channelIdx < AAC_CHANNELS.length) {
                    channels = AAC_CHANNELS[channelIdx];
                    int size = fourth & 0x3;
                    size <<= 8;
                    size += (data.readByte() & 0xFF);
                    size <<= 3;
                    size += (data.readByte() >> 5 & 0x7);
                    if (size > 0) {
                        int rdb = data.readByte() & 0x3;
                        aacFrame = new AACFrame();
                        aacFrame.setChannelConfiguration(channelIdx);
                        aacFrame.setChannels(channels);
                        aacFrame.setRdb(rdb);
                        aacFrame.setSamplingFrequencyIndex(rateIdx);
                        aacFrame.setSampleRate(rate);
                        aacFrame.setSize(size);
                        int dataLen = size - 7;// adts header 7 byte
                        if (data.readableBytes() >= dataLen) {
                            byte[] buffer = new byte[dataLen];
                            data.readBytes(buffer);
                            aacFrame.setData(Unpooled.wrappedBuffer(buffer));
                        }
                        aacFrame.setErrorBitsAbsent(errorBitsAbsent);
                        aacFrame.setAudioObjectType(profileType);
                    }
                }
            }
        }
        return aacFrame;
    }

    public static AACFrame decodeAACCodecConfig(BufFacade buffer) {

        // aacCodecConfig need 2 byte
        if (buffer.readableBytes() < 2) return null;
        byte first = buffer.readByte();
        byte second = buffer.readByte();

        int audioObjectType = (first >> 3) & 0x1f;  //audioObjectType

        int samplingFrequencyIndex = (first & 0x07) << 1;
        samplingFrequencyIndex += second >> 7 & 0x01;          //samplingFrequencyIndex

        int channelConfiguration = (second & 0x7f) >> 3;  //channelConfiguration

        int rate = 0;
        int channels = 0;

        if (samplingFrequencyIndex >= 0 && samplingFrequencyIndex < AAC_SAMPLERATES.length) {
            rate = AAC_SAMPLERATES[samplingFrequencyIndex];
        }

        if (channelConfiguration >= 0 && channelConfiguration < AAC_CHANNELS.length) {
            channels = AAC_CHANNELS[channelConfiguration];
        }

        AACFrame aacFrame = null;
        aacFrame = new AACFrame();
        aacFrame.setAudioObjectType(audioObjectType);
        aacFrame.setChannelConfiguration(channelConfiguration);
        aacFrame.setChannels(channels);
        aacFrame.setSamplingFrequencyIndex(samplingFrequencyIndex);
        aacFrame.setSampleRate(rate);
        return aacFrame;
    }

    public static BufFacade frameToADTSBuffer(AACFrame aacFrame) {
        byte[] adts = new byte[7];
        frameToADTSBuffer(aacFrame, adts, 0);
        return BufFacade.wrappedBuffer(adts);
    }

    /**
     * Letter 	Length (bits) 	Description
     * A 		12 	syncword 0xFFF, all bits must be 1
     * B 		1 	MPEG Version: 0 for MPEG-4, 1 for MPEG-2
     * C 		2 	Layer: always 0
     * D 		1 	protection absent, Warning, set to 1 if there is no CRC and 0 if there is CRC
     * E 		2 	profile, the MPEG-4 Audio Object Type minus 1
     * F 		4 	MPEG-4 Sampling Frequency Index (15 is forbidden)
     * G 		1 	private stream, set to 0 when encoding, ignore when decoding
     * H 		3 	MPEG-4 Channel Configuration (in the case of 0, the channel configuration is sent via an inband PCE)
     * I 		1 	originality, set to 0 when encoding, ignore when decoding
     * J 		1 	home, set to 0 when encoding, ignore when decoding
     * K 		1 	copyrighted stream, set to 0 when encoding, ignore when decoding
     * L 		1 	copyright start, set to 0 when encoding, ignore when decoding
     * M 		13 	frame length, this value must include 7 or 9 bytes of header length: FrameLength = (ProtectionAbsent == 1 ? 7 : 9) + size(AACFrame)
     * O 		11 	Buffer fullness
     * P 		2 	Number of AAC frames (RDBs) in ADTS frame minus 1, for maximum compatibility always use 1 AAC frame per ADTS frame
     * Q 		16 	CRC if protection absent is 0
     *
     * @param aacFrame
     * @param adts
     * @param startIdx
     */
    public static void frameToADTSBuffer(AACFrame aacFrame, byte[] adts, int startIdx) {
        int samplingFrequencyIndex = aacFrame.getSamplingFrequencyIndex();
        int audioObjectType = aacFrame.getAudioObjectType();
        int channelConfiguration = aacFrame.getChannelConfiguration();

        int size = aacFrame.getSize();
        boolean isError = aacFrame.isErrorBitsAbsent();

        int SyncWord = 0xFFF;  //12b
        adts[(startIdx + 0)] = (byte) (SyncWord >> 4 & 0xFF);


        int MPEGVersion = 0x00;   //1b
        int Layer = 0x00;           //2b
        int ProtectionAbsent = isError ? 0x01 : 0x00;  //1b

        int byteSecond = ((SyncWord & 0x0F) << 4) + (MPEGVersion << 3) + (Layer << 1) + ProtectionAbsent;
        adts[(startIdx + 1)] = (byte) byteSecond;

        int Profile = audioObjectType - 1;

        int MPEG4SamplingFrequencyIndex = samplingFrequencyIndex;

        int PrivateStream = 0x00;

        int MPEG4ChannelConfiguration = channelConfiguration;

        adts[(startIdx + 2)] = (byte) ((Profile << 6) + ((MPEG4SamplingFrequencyIndex << 2) & 0x3C)
                + ((PrivateStream << 1) & 0x1)
                + (MPEG4ChannelConfiguration >> 2 & 0x1));


        int Originality = 0x00;
        int Home = 0x00;
        int CopyrightedStream = 0x00;
        int CopyrightedStart = 0x00;
        int FrameLength = size;

        adts[(startIdx + 3)] = (byte) (((MPEG4SamplingFrequencyIndex & 0x3) << 6) + (Originality << 5) + (Home << 4)
                + (CopyrightedStream << 3)
                + (CopyrightedStart << 2)
                + (FrameLength >> 11 & 0x3)
        );

        adts[(startIdx + 4)] = (byte) (FrameLength >> 3 & 0xFF);

        int BufferFullness = 2047;  // 11

        adts[(startIdx + 5)] = (byte) (((FrameLength & 0x07) << 5) + (BufferFullness >> 6));

        int NumberOfAACFrames = aacFrame.getRdb(); //2
        adts[(startIdx + 6)] = (byte) ((BufferFullness << 2 & 0xFF) + (NumberOfAACFrames & 0x3));
    }
}
