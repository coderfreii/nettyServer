package org.tl.nettyServer.media.net.rtsp;

import gov.nist.javax.sdp.MediaDescriptionImpl;
import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.parser.SDPAnnounceParser;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.RtspServer;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.media.flv.FLVUtils;
import org.tl.nettyServer.media.net.http.codec.HTTPCodecUtil;
import org.tl.nettyServer.media.net.http.codec.QueryStringDecoder;
import org.tl.nettyServer.media.net.http.request.HTTPRequest;
import org.tl.nettyServer.media.net.http.response.HTTPResponse;
import org.tl.nettyServer.media.net.rtsp.conn.RTSPConnectionConsumer;
import org.tl.nettyServer.media.net.rtsp.conn.RTSPMinaConnection;
import org.tl.nettyServer.media.net.rtsp.message.RTSPHeaders;
import org.tl.nettyServer.media.net.rtsp.message.RTSPResponseStatuses;
import org.tl.nettyServer.media.net.rtsp.rtp.RTPPlayer;
import org.tl.nettyServer.media.net.rtsp.rtp.RTPUtil;
import org.tl.nettyServer.media.net.rtsp.rtp.packetizer.*;
import org.tl.nettyServer.media.net.udp.UDPPortManager;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.scope.Scope;
import org.tl.nettyServer.media.service.IStreamSecurityService;
import org.tl.nettyServer.media.stream.client.CustomSingleItemSubStream;
import org.tl.nettyServer.media.stream.playlist.SimplePlayItem;
import org.tl.nettyServer.media.stream.proxy.RTSPPushProxyStream;
import org.tl.nettyServer.media.stream.support.IStreamPlaybackSecurity;
import org.tl.nettyServer.media.util.ScopeUtils;

import javax.sdp.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * rtsp protocol process
 *
 * @author pengliren
 */
public final class RTSPCore {

    private static Logger log = LoggerFactory.getLogger(RTSPCore.class);

    private static Pattern rtspTransportPattern = Pattern.compile(".*client_port=(\\d*)-(\\d*).*");

    public static ConcurrentHashMap<String, RTPPlayer> rtpSocketMaps = new ConcurrentHashMap<String, RTPPlayer>();


    /**
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    public static boolean handleRtspMethod(HTTPRequest request, HTTPResponse response) throws Exception {

        boolean flag = true;
        RTSPMinaConnection conn = (RTSPMinaConnection) Red5.getConnectionLocal();
        response.setHeader(RTSPHeaders.Names.SESSION, String.format("%s;%s", conn.getSessionId(), "timeout=60"));
        response.setHeader(RTSPHeaders.Names.SERVER, "RED");
        response.setHeader(RTSPHeaders.Names.CSEQ, String.valueOf(request.getHeader("CSeq")));
        response.setHeader(RTSPHeaders.Names.CACHE_CONTROL, "no-cache");
        if (request.getMethod() == RTSPMethods.OPTIONS) {
            RTSPCore.options(request, response);
        } else if (request.getMethod() == RTSPMethods.DESCRIBE) {
            RTSPCore.describe(request, response);
        } else if (request.getMethod() == RTSPMethods.ANNOUNCE) {
            RTSPCore.announce(request, response);
        } else if (request.getMethod() == RTSPMethods.SETUP) {
            RTSPCore.setup(request, response);
        } else if (request.getMethod() == RTSPMethods.PLAY) {
            RTSPCore.play(request, response);
        } else if (request.getMethod() == RTSPMethods.PAUSE) {
            RTSPCore.pause(request, response);
        } else if (request.getMethod() == RTSPMethods.TEARDOWN) {
            RTSPCore.teardown(request, response);
        } else if (request.getMethod() == RTSPMethods.GET_PARAMETER) {
            RTSPCore.getParameter(request, response);
        } else if (request.getMethod() == RTSPMethods.SET_PARAMETER) {
            RTSPCore.setParameter(request, response);
        } else if (request.getMethod() == RTSPMethods.REDIRECT) {
            RTSPCore.redirect(request, response);
        } else if (request.getMethod() == RTSPMethods.RECORD) {
            RTSPCore.record(request, response);
        } else {
            flag = false;
        }

        return flag;
    }

    /**
     * rtsp options method
     *
     * @param request
     * @param response
     */
    public static void options(HTTPRequest request, HTTPResponse response) {

        response.setHeader("Public", "DESCRIBE,SETUP,TEARDOWN,PLAY,PAUSE,OPTIONS,ANNOUNCE,RECORD,GET_PARAMETER");
        response.setHeader("Supported", "play.basic,con.persistent");
    }

    /**
     * rtsp describe method
     *
     * @param request
     * @param response
     */
    public static void describe(HTTPRequest request, HTTPResponse response) {

        try {
            RTSPMinaConnection conn = (RTSPMinaConnection) Red5.getConnectionLocal();
            RTSPConnectionConsumer rtspConsumer = createPlayStream(request.getUri());
            if (rtspConsumer == null) {
                response.setStatus(RTSPResponseStatuses.NOT_FOUND);
                return;
            }
            conn.setAttribute("rtspConsumer", rtspConsumer);
            response.addHeader(RTSPHeaders.Names.CONTENT_TYPE, "application/sdp");
            String sdp = configureSDP();
            response.addHeader(RTSPHeaders.Names.CONTENT_LENGTH, sdp.length());
            response.setContent(BufFacade.wrapperAndCast(HTTPCodecUtil.encodeBody(sdp)));
        } catch (Exception e) {
            e.printStackTrace();
            log.info("describe exception {}", e.toString());
            response.setStatus(RTSPResponseStatuses.BAD_REQUEST);
        }
    }

    /**
     * rtsp method announce
     *
     * @param request
     * @param response
     * @throws Exception
     */
    public static void announce(HTTPRequest request, HTTPResponse response) throws Exception {

        RTSPMinaConnection conn = (RTSPMinaConnection) Red5.getConnectionLocal();
        // make sure we only handle application/sdp
        String contentType = request.getHeader(RTSPHeaders.Names.CONTENT_TYPE);
        if (contentType == null || !contentType.equalsIgnoreCase("application/sdp")) {
            response.setStatus(RTSPResponseStatuses.BAD_REQUEST);
            return;
        }
        // get the sdp
        String sdp = HTTPCodecUtil.decodeBody((ByteBuf) request.getContent().getBuf());
        // get the first video track and audio track
        if (sdp != null) {
            SDPAnnounceParser parser = new SDPAnnounceParser(sdp);
            SessionDescriptionImpl sessiondescription = parser.parse();

            RTSPPushProxyStream stream = createPublishStream(request.getUri(), conn);
            conn.setAttribute("pushStream", stream);
            conn.setAttribute("sdp", sessiondescription);
            conn.setAttribute("isInbound", true);
        } else {
            response.setStatus(RTSPResponseStatuses.BAD_REQUEST);
        }
    }

    /**
     * rtsp method getParameter
     *
     * @param request
     * @param response
     */
    public static void getParameter(HTTPRequest request, HTTPResponse response) {

        //System.out.println("getParameter: "+request);
    }

    /**
     * rtsp method setParameter
     *
     * @param request
     * @param response
     */
    public static void setParameter(HTTPRequest request, HTTPResponse response) {

        //System.out.println("setParameter: "+request);
    }

    /**
     * rtsp method pause
     *
     * @param request
     * @param response
     */
    public static void pause(HTTPRequest request, HTTPResponse response) {

        RTSPMinaConnection conn = (RTSPMinaConnection) Red5.getConnectionLocal();
        CustomSingleItemSubStream rtspStream = (CustomSingleItemSubStream) conn.getAttribute("rtspStream");
        if (rtspStream != null) {
            rtspStream.pause(0);
        }
        log.info("stream pause {}", request.getUri());
    }

    /**
     * rtsp method play
     *
     * @param request
     * @param response
     * @throws IOException
     */
    public static void play(HTTPRequest request, HTTPResponse response) throws Exception {

        RTSPMinaConnection conn = (RTSPMinaConnection) Red5.getConnectionLocal();
        final RTSPConnectionConsumer rtspConsumer = (RTSPConnectionConsumer) conn.getAttribute("rtspConsumer");
        if (rtspConsumer == null) {
            response.setStatus(RTSPResponseStatuses.NOT_FOUND);
            return;
        }

        CustomSingleItemSubStream rtspStream = (CustomSingleItemSubStream) conn.getAttribute("rtspStream");
        double[] rangeNtp = RTPUtil.decodeRangeHeader(request.getHeader(RTSPHeaders.Names.RANGE));
        if (rangeNtp[0] > 0) {
            log.info("rtsp seekTo {}", rangeNtp[0]);
            rtspStream.seek(Math.round((float) (rangeNtp[0] * 1000)));
        }
        //RTP-Info: url=rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov/trackID=1;seq=1;rtptime=0,url=rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov/trackID=2;seq=1;rtptime=0
        log.info("rtsp play {} by type {}", request.getUri(), (conn.getPlayType() == 0 ? "udp" : "tcp"));
        if (conn.hasAttribute("playing")) {
            int pos = 0;
            if (!conn.isLive()) pos = Math.round((float) (rangeNtp[0] * 1000));
            rtspStream.resume(pos);
            return;
        }
        // first play
        RTPPlayer player = conn.getRtpConnector();
        if (player != null) {
            rtspConsumer.addStreamListener(player);
        }
        conn.setAttribute("playing", true);
    }

    /**
     * rtsp record method
     *
     * @param request
     * @param response
     */
    public static void record(HTTPRequest request, HTTPResponse response) {

        RTSPMinaConnection conn = (RTSPMinaConnection) Red5.getConnectionLocal();
        RTSPPushProxyStream pushStream = (RTSPPushProxyStream) conn.getAttribute("pushStream");
        if (pushStream != null) {
            pushStream.start();

            BufFacade videoConfig = (BufFacade) conn.getAttribute("videoConfig");
            if (videoConfig != null) {
                pushStream.setAVCConfig(videoConfig);
                pushStream.setVideoTimescale((Integer) conn.getAttribute("rtpmapVideoTimescale"));
            }

            BufFacade audioConfig = (BufFacade) conn.getAttribute("audioConfig");
            if (audioConfig != null) {
                pushStream.setAACConfig(audioConfig);
                pushStream.setAudioTimescale((Integer) conn.getAttribute("rtpmapAudioTimescale"));
            }
        }
    }

    /**
     * rtsp redirect method
     *
     * @param request
     * @param response
     */
    public static void redirect(HTTPRequest request, HTTPResponse response) {

    }

    /**
     * rtsp setup method
     *
     * @param request
     * @param response
     * @throws Exception
     */
    public static void setup(HTTPRequest request, HTTPResponse response) throws Exception {

        RTSPMinaConnection conn = (RTSPMinaConnection) Red5.getConnectionLocal();
        if (conn.getAttribute("isInbound") != null) {
            setupInbound(request, response);
        } else {
            setupOutbound(request, response);
        }
    }

    /**
     * rtsp setup method used of publish
     *
     * @param request
     * @param response
     * @throws Exception
     */
    private static void setupInbound(HTTPRequest request, HTTPResponse response) throws Exception {

        RTSPMinaConnection conn = (RTSPMinaConnection) Red5.getConnectionLocal();
        Matcher m = rtspTransportPattern.matcher(request.getHeader("Transport"));
        SessionDescriptionImpl sessiondescription = (SessionDescriptionImpl) conn.getAttribute("sdp");
        String mediaType = null;
        String videoControl = null;
        String audioControl = null;
        BufFacade videoConfig = null;
        BufFacade audioConfig = null;
        String rtpmap;
        String fmtp;
        Vector<MediaDescription> descs = sessiondescription.getMediaDescriptions(false);
        for (MediaDescription desc : descs) {
            mediaType = desc.getMedia().getMediaType();
            rtpmap = desc.getAttribute("rtpmap");
            fmtp = desc.getAttribute("fmtp");
            if (mediaType.equalsIgnoreCase("video")) {
                videoControl = desc.getAttribute("control");
                videoConfig = RTPUtil.decodeVideoConfigure(rtpmap, fmtp);
                conn.setAttribute("videoConfig", videoConfig);
                conn.setAttribute("rtpmapVideoTimescale", RTPUtil.decodeRtpmapVideoTimescale(rtpmap));
            } else if (mediaType.equalsIgnoreCase("audio")) {
                audioControl = desc.getAttribute("control");
                audioConfig = RTPUtil.decodeAudioConfigure(rtpmap, fmtp);
                conn.setAttribute("audioConfig", audioConfig);
                conn.setAttribute("rtpmapAudioTimescale", RTPUtil.decodeRtpmapAudioTimescale(rtpmap));
            }
        }

        // must video is avc and audio is aac
        if (videoConfig == null && audioConfig == null) {
            response.setStatus(RTSPResponseStatuses.BAD_REQUEST);
            return;
        }

        if (StringUtils.isEmpty(videoControl) && StringUtils.isEmpty(audioControl)) {
            response.setStatus(RTSPResponseStatuses.BAD_REQUEST);
            return;
        }

        if (m.matches()) { // udp is unsupported
            response.setStatus(RTSPResponseStatuses.UNSUPPORTED_TRANSPORT);
        } else { // tcp is support
            if (request.getUri().endsWith("streamid=0")) {
                response.setHeader("Transport", "RTP/AVP/TCP;unicast;interleaved=0-1");//video
            } else if (request.getUri().endsWith("streamid=1")) {
                response.setHeader("Transport", "RTP/AVP/TCP;unicast;interleaved=2-3");//audio
            }
        }
    }

    /**
     * rtsp setup metod used of play
     *
     * @param request
     * @param response
     * @throws Exception
     */
    private static void setupOutbound(HTTPRequest request, HTTPResponse response) throws Exception {

        RTSPMinaConnection conn = (RTSPMinaConnection) Red5.getConnectionLocal();
        Matcher m = rtspTransportPattern.matcher(request.getHeader("Transport"));
        RTPPlayer player = conn.getRtpConnector();
        if (m.matches()) { // udp
            InetSocketAddress localAddr = ((InetSocketAddress) conn.getRtspSession().getLocalAddress());
            String host = localAddr.getAddress().getHostAddress();
            conn.setPlayType(RTSPMinaConnection.PLAY_TYPE_UDP);
            int rtpPort = Integer.valueOf(m.group(1));
            int rtcpPort = Integer.valueOf(m.group(2));
            StringBuilder serverTransport = new StringBuilder();
            UDPPortManager udpPortMgr = UDPPortManager.getInstance();
            InetSocketAddress remoteAddress = (InetSocketAddress) conn.getRtspSession().getRemoteAddress();
            if (request.getUri().endsWith("trackID=0")) {
                int[] pair = udpPortMgr.expandToPortPair(udpPortMgr.acquireUDPPortPair());
                conn.setVideoPairPort(pair);
                while (!bindVideoPort(pair)) {
                    pair = udpPortMgr.expandToPortPair(udpPortMgr.acquireUDPPortPair());
                }
                InetSocketAddress rtpVideoAddress = new InetSocketAddress(remoteAddress.getAddress(), rtpPort);
                InetSocketAddress rtcpVideoAddress = new InetSocketAddress(remoteAddress.getAddress(), rtcpPort);
                player.setVideoRtpAddress(rtpVideoAddress);
                player.getVideoRtpPacketizer().initRtcpInfo(rtcpVideoAddress);
                // handle mpegts
                if (conn.isMpegts()) {
                    player.setAudioRtpAddress(rtpVideoAddress);
                    player.getAudioRtpPacketizer().initRtcpInfo(rtcpVideoAddress);
                }
                rtpSocketMaps.put(String.format("%s:%d", remoteAddress.getAddress().getHostAddress(), rtcpPort), player);
                serverTransport.append(request.getHeader("Transport"))
                        .append(";source=").append(host)
                        .append(";server_port=").append(pair[0]).append("-")
                        .append(pair[1]);
            } else if (request.getUri().endsWith("trackID=1")) {
                int[] pair = udpPortMgr.expandToPortPair(udpPortMgr.acquireUDPPortPair());
                conn.setAudioPairPort(pair);
                while (!bindAudioPort(pair)) {
                    pair = udpPortMgr.expandToPortPair(udpPortMgr.acquireUDPPortPair());
                }
                InetSocketAddress rtpAudioAddress = new InetSocketAddress(remoteAddress.getAddress(), rtpPort);
                InetSocketAddress rtcpAudioAddress = new InetSocketAddress(remoteAddress.getAddress(), rtcpPort);
                player.setAudioRtpAddress(rtpAudioAddress);
                player.getAudioRtpPacketizer().initRtcpInfo(rtcpAudioAddress);
                rtpSocketMaps.put(String.format("%s:%d", remoteAddress.getAddress().getHostAddress(), rtcpPort), player);
                serverTransport.append(request.getHeader("Transport"))
                        .append(";source=").append(host)
                        .append(";server_port=").append(pair[0]).append("-")
                        .append(pair[1]);
            }
            response.setHeader("Transport", serverTransport.toString());

        } else { //tcp
            conn.setPlayType(RTSPMinaConnection.PLAY_TYPE_TCP);
            if (request.getUri().endsWith("trackID=0")) {
                response.setHeader("Transport", String.format("RTP/AVP/TCP;unicast;interleaved=0-1;ssrc=%d", player.getVideoRtpPacketizer().getSsrc()));//视频
            } else if (request.getUri().endsWith("trackID=1")) {
                response.setHeader("Transport", String.format("RTP/AVP/TCP;unicast;interleaved=2-3;ssrc=%d", player.getAudioRtpPacketizer().getSsrc()));//音频
            }
        }
    }

    /**
     * bind video udp transport port include rtp and rtcp
     *
     * @param pair
     * @return
     */
    private static boolean bindVideoPort(int[] pair) {
        boolean flag = true;
        RtspServer.RTP_VIDEO_ACCEPTOR_CHANNEL = RtspServer.RTP_VIDEO_ACCEPTOR.bind(new InetSocketAddress("127.0.0.1", pair[0])).channel();
        RtspServer.RTCP_VIDEO_ACCEPTOR_CHANNEL = RtspServer.RTCP_VIDEO_ACCEPTOR.bind(new InetSocketAddress("127.0.0.1", pair[1])).channel();
        return flag;
    }

    /**
     * bind audio udp transport port include rtp and rtcp
     *
     * @param pair
     * @return
     */
    private static boolean bindAudioPort(int[] pair) {

        boolean flag = true;
        RtspServer.RTP_AUDIO_ACCEPTOR_CHANNEL = RtspServer.RTP_AUDIO_ACCEPTOR.bind(new InetSocketAddress("127.0.0.1", pair[0])).channel();
        RtspServer.RTCP_AUDIO_ACCEPTOR_CHANNEL = RtspServer.RTCP_AUDIO_ACCEPTOR.bind(new InetSocketAddress("127.0.0.1", pair[1])).channel();
        return flag;
    }

    public static void teardown(HTTPRequest request, HTTPResponse response) {

        RTSPMinaConnection conn = (RTSPMinaConnection) Red5.getConnectionLocal();
        conn.close();
    }

    /**
     * by avc config and aac config product sdp
     *
     * @return
     * @throws SdpException
     */
    public static String configureSDP() throws Exception {

        RTSPMinaConnection conn = (RTSPMinaConnection) Red5.getConnectionLocal();
        SessionDescription sdp;
        MediaDescriptionImpl mdvideo = null;//video md
        MediaDescriptionImpl mdaudio = null;//audio md
        Vector<MediaDescription> mds = new Vector<MediaDescription>(1);
        Vector<Attribute> attr = new Vector<Attribute>();
        sdp = SdpFactory.getInstance().createSessionDescription();
        InetSocketAddress remoteAddress = (InetSocketAddress) conn.getRtspSession().getRemoteAddress();
        String remoteIp = "";
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            remoteIp = remoteAddress.getAddress().toString().substring(1);
        }
        sdp.setOrigin(SdpFactory.getInstance().createOrigin("Red5", Long.valueOf(conn.getSessionId()), 1, "IN", "IPV4", remoteIp));
        sdp.setAttributes(attr);
        sdp.setConnection(SdpFactory.getInstance().createConnection("IN", "IPV4", remoteIp));
        CustomSingleItemSubStream rtspStream = (CustomSingleItemSubStream) conn.getAttribute("rtspStream");

        BufFacade data = null;
        byte codecId;
        RTPPlayer player;
        IRTPPacketizer videoRtpPacketizer = null;
        IRTPPacketizer audioRtpPacketizer = null;

        BufFacade videoConfig = BufFacade.buffer(128);
        BufFacade audioConfig = BufFacade.buffer(128);
        AtomicLong duration = new AtomicLong(0);
        rtspStream.getConfig(videoConfig, audioConfig, duration);
        if (duration.get() == 0) {
            attr.add(SdpFactory.getInstance().createAttribute("range", "npt=now-"));
            conn.setLive(true);
        } else {
            attr.add(SdpFactory.getInstance().createAttribute("range", "npt=0-" + (double) duration.get() / 1000D));
            conn.setLive(false);
        }

        if (conn.isMpegts()) {
            videoRtpPacketizer = new RTPPacketizerMPEGTS(videoConfig, audioConfig);
            audioRtpPacketizer = videoRtpPacketizer;
            mdvideo = videoRtpPacketizer.getDescribeInfo(data);
            if (mdvideo != null) {
                mdvideo.setAttribute("control", "trackID=0");
                mds.add(mdvideo);
            }
        } else {
            // configure video sdp
            data = videoConfig.asReadOnly();
            if (data.readable()) {
                data.markReaderIndex();
                codecId = (byte) FLVUtils.getVideoCodec(data.readByte());
                switch (codecId) {
                    case 0x07: //avc/h.264 video
                        videoRtpPacketizer = new RTPPacketizerRFC3984H264();
                        break;
                }
                data.readerIndex(5);
                if (videoRtpPacketizer != null) {
                    mdvideo = videoRtpPacketizer.getDescribeInfo(data);
                }

                if (mdvideo != null) {
                    mdvideo.setAttribute("control", "trackID=0");
                    mds.add(mdvideo);
                }
                data.resetReaderIndex();
            }

            // configure audio sdp
            data = audioConfig.asReadOnly();
            if (data.readable()) {
                data.markReaderIndex();
                codecId = (byte) FLVUtils.getAudioCodec(data.readByte());
                switch (codecId) {
                    case 0x02: //mp3
                        audioRtpPacketizer = new RTPPacketizerRFC2250MP3();
                        break;
                    case 0x0a: //aac
                        audioRtpPacketizer = new RTPPacketizerMPEG4AAC();
                        break;
                }
                data.skipBytes(1);
                if (audioRtpPacketizer != null) {
                    mdaudio = audioRtpPacketizer.getDescribeInfo(data);
                }

                if (mdaudio != null) {
                    mdaudio.setAttribute("control", "trackID=1");
                    mds.add(mdaudio);
                }
                data.resetReaderIndex();
            }
        }
        player = new RTPPlayer(conn, videoRtpPacketizer, audioRtpPacketizer);
        conn.setRtpConnector(player);
        sdp.setMediaDescriptions(mds);
        return sdp.toString();
    }

    /**
     * parse rtsp url and create publish stream
     *
     * @param url
     * @return
     * @throws URISyntaxException
     */
    private static RTSPPushProxyStream createPublishStream(String url, RTSPMinaConnection conn) throws URISyntaxException {

        URI uri = new URI(url);
        String[] segments = uri.getPath().substring(1).split("/");
        if (segments.length < 2) return null;

        String app = segments[0];
        String stream = segments[1];
        IScope scope = conn.getScope();//ScopeUtils.getScope(app);
        if (scope == null || StringUtils.isEmpty(stream)) return null;

        RTSPPushProxyStream pubStream = new RTSPPushProxyStream(stream);
        pubStream.setScope(scope);
        pubStream.setPublishedName(stream);
        return pubStream;
    }

    /**
     * parse rtsp url and create play stream
     *
     * @param url
     * @return
     * @throws URISyntaxException
     */
    private static RTSPConnectionConsumer createPlayStream(String url) throws URISyntaxException {

        RTSPMinaConnection conn = (RTSPMinaConnection) Red5.getConnectionLocal();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(url);
        URI uri = new URI(queryStringDecoder.getPath());
        String[] segments = uri.getPath().substring(1).split("/");
        if (segments.length < 2) return null;
        String stream = segments[1];
        Scope scope = (Scope) conn.getScope();
		/*if(scope == null) {
			scope = ScopeUtils.getScope(segments[0]);
			if(scope == null) { // root scope?
				scope = ScopeUtils.getScope("root");
			} 
		} else {// root scope
			scope = ScopeUtils.getScope("root");
		}  */

        if (scope == null || StringUtils.isEmpty(stream)) return null;

        StringBuilder streamSb = new StringBuilder();
        streamSb.append(stream);
        boolean firstParam = true;
        // fix stream
        // rtsp://192.168.10.123:5544/live/12345?tcp&mpegts&starttime=12345
        if (!queryStringDecoder.getParameters().isEmpty()) {
            Map<String, List<String>> params = queryStringDecoder.getParameters();
            for (String key : params.keySet()) {
                if (key.equalsIgnoreCase("tcp")) {
                    continue;
                } else if (key.equalsIgnoreCase("mpegts")) {
                    conn.setMpegts(true);
                } else {
                    if (params.get(key).size() > 0) {
                        if (firstParam) {
                            streamSb.append("?");
                            firstParam = false;
                        } else {
                            streamSb.append("&");
                        }
                        streamSb.append(key).append("=").append(params.get(0));
                    }
                }
            }
        }

        // play security
        IStreamSecurityService security =
                (IStreamSecurityService) ScopeUtils.getScopeService(scope, IStreamSecurityService.class);
        if (security != null) {
            Set<IStreamPlaybackSecurity> handlers = security.getStreamPlaybackSecurity();
            for (IStreamPlaybackSecurity handler : handlers) {
                if (!handler.isPlaybackAllowed(scope, stream, 0, 0, false)) {
                    return null;
                }
            }
        }

        // set up stream
        RTSPConnectionConsumer rtspConsumer = new RTSPConnectionConsumer(conn);
        rtspConsumer.getConnection().connect(scope);
        CustomSingleItemSubStream rtspStream = new CustomSingleItemSubStream(scope, rtspConsumer);
        SimplePlayItem playItem = SimplePlayItem.build(stream, -2000, -1);
        rtspStream.setPlayItem(playItem);
        rtspStream.start();

        conn.setAttribute("rtspStream", rtspStream);

        try {
            rtspStream.play();
        } catch (Exception e) {
            rtspStream.stop();
            return null;
        }

        if (rtspStream.isFailure()) {
            rtspStream.stop();
            return null;
        }

        return rtspConsumer;
    }
}
