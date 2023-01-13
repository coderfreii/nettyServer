package org.tl.nettyServer.media.net.hls.service;

import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.client.ScopeContextBean;
import org.tl.nettyServer.media.conf.ExtConfiguration;
import org.tl.nettyServer.media.io.IStreamableFile;
import org.tl.nettyServer.media.io.ITagReader;
import org.tl.nettyServer.media.media.flv.IKeyFrameDataAnalyzer;
import org.tl.nettyServer.media.net.hls.ts.message.MpegTsSegment;
import org.tl.nettyServer.media.net.hls.ts.service.MpegTsSegmentService;
import org.tl.nettyServer.media.net.http.conn.HTTPConnection;
import org.tl.nettyServer.media.net.http.request.HTTPRequest;
import org.tl.nettyServer.media.net.http.response.HTTPResponse;
import org.tl.nettyServer.media.net.http.response.HTTPResponseStatus;
import org.tl.nettyServer.media.net.http.service.BaseHTTPService;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.service.IStreamableFileService;
import org.tl.nettyServer.media.service.IStreamableFileServiceFactory;
import org.tl.nettyServer.media.service.StreamableFileServiceFactory;
import org.tl.nettyServer.media.service.provider.IProviderService;
import org.tl.nettyServer.media.util.ScopeUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.tl.nettyServer.media.net.http.message.HTTPHeaders.Names.CONTENT_TYPE;


/**
 * HTTP Live Stream M3U8 File Parse
 *
 * @author pengliren
 * 播放过程分为直播（live）和点播（vod）
 */
@Slf4j
public class HTTPM3U8Service extends BaseHTTPService {

    @Override
    public void handleRequest(HTTPRequest req, HTTPResponse resp, IScope scope) throws Exception {

        String method = req.getMethod().toString();

        if (!REQUEST_GET_METHOD.equalsIgnoreCase(method) && !REQUEST_POST_METHOD.equalsIgnoreCase(method)) {
            // Bad request - return simple error page
            sendError(req, resp, HTTPResponseStatus.BAD_REQUEST);
            return;
        }

        String path = req.getPath().substring(1);
        String[] segments = path.split("/");


        if (segments.length < 2) { // app/stream/playlist.m3u8
            sendError(req, resp, HTTPResponseStatus.BAD_REQUEST);
            return;
        }

        String streamName = segments[0];
        IProviderService providerService = (IProviderService) scope.getContext().getBean(ScopeContextBean.PROVIDERSERVICE_BEAN);
        IProviderService.INPUT_TYPE result = providerService.lookupProviderInput(scope, streamName, 0);


        String app = scope.getName();
        if (result == IProviderService.INPUT_TYPE.VOD) {
            playVodStream(scope, app, streamName, req, resp);
        } else if (result == IProviderService.INPUT_TYPE.LIVE) {
            playLiveStream(scope, app, streamName, req, resp);
        }
    }

    /**
     * play live stream by hls
     *
     * @param scope
     * @param app
     * @param streamName
     * @param resp
     */
    private void playLiveStream(IScope scope, String app, String streamName, HTTPRequest req, HTTPResponse resp) {
        HTTPConnection conn = (HTTPConnection) Red5.getConnectionLocal();
        MpegTsSegmentService service = MpegTsSegmentService.getInstance();

        StringBuilder buff = new StringBuilder();
        buff.append("#EXTM3U\n")
                .append("#EXT-X-VERSION:3\n")
                .append("#EXT-X-ALLOW-CACHE:NO\n");

        if (service.isAvailable(scope, streamName)) {
            int count = service.getSegmentCount(app, streamName);
            if (count == 0) {
                long maxWaitTime = 2 * service.getSegmentTimeLimit();
                long start = System.currentTimeMillis();
                do {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        log.info("waiting thread interrupted?");
                        break;
                    }
                    if ((System.currentTimeMillis() - start) >= maxWaitTime) {
                        log.info("Maximum segment wait time exceeded for {}", streamName);
                        break;
                    }
                } while ((count = service.getSegmentCount(app, streamName)) < 1);
            }

            // get the count one last time
            count = service.getSegmentCount(app, streamName);
            if (count >= 1) {
                //get segment duration in seconds
                long segmentDuration = service.getSegmentTimeLimit() / 1000;
                //get the current segment
                List<MpegTsSegment> tsSegments = service.getSegmentList(app, streamName);
                //get current sequence number  use first one sequence present sequence of playlist.m3u8
                int sequenceNumber = tsSegments.get(0).getSequence();
                // create the heading
                buff.append(String.format("#EXT-X-TARGETDURATION:%s\n#EXT-X-MEDIA-SEQUENCE:%s\n", segmentDuration, sequenceNumber));

                // if segment is encrypt append encrypt key address
                if (service.getSegmentIsEncrypt(app, streamName)) {
                    if (conn.getHttpSession().getLocalAddress() == null) return;
                    String address = conn.getHttpSession().getLocalAddress().toString();
                    String httpAes = String.format("http:/%s/%s/%s/aes", address, app, streamName);
                    buff.append("#EXT-X-KEY:METHOD=AES-128,URI=\"").append(httpAes).append("\"\n");
                }

                for (MpegTsSegment seg : tsSegments) {
                    buff.append(String.format("#EXTINF:%s, \n%s.ts?type=live\n", segmentDuration, seg.getSequence()));
                }
            } else {
                log.info("Minimum segment count not yet reached, currently at: {}", count);
            }
        } else {
            log.info("Stream: {} is not available", streamName);
            buff.append("#EXT-X-ENDLIST\n");
        }
        BufFacade data = BufFacade.buffer(128).writeBytes(buff.toString().getBytes());
        setHeader(resp);
        commitResponse(req, resp, data);
    }

    /**
     * vod一般指视频点播。英文称为 Video on Demand，所以也称为VOD
     * play vod stream by hls and file support flv format
     *
     * @param scope
     * @param app
     * @param streamName
     * @param resp
     * @throws IOException
     */
    private void playVodStream(IScope scope, String app, String streamName, HTTPRequest req, HTTPResponse resp) {
        IProviderService providerService = (IProviderService) scope.getContext().getBean(ScopeContextBean.PROVIDERSERVICE_BEAN);
        File file = providerService.getVODProviderFile(scope, streamName);

        if (file != null && file.exists()) {
            IStreamableFileServiceFactory factory = (IStreamableFileServiceFactory) ScopeUtils.getScopeService(scope, IStreamableFileServiceFactory.class, StreamableFileServiceFactory.class);
            IStreamableFileService service = factory.getService(file);
            if (service != null) {
                ITagReader reader;
                try {
                    IStreamableFile streamFile = service.getStreamableFile(file);
                    reader = streamFile.getReader();
                } catch (IOException e) {
                    log.info("play hls exception {}", e.getMessage());
                    sendError(req, resp, HTTPResponseStatus.BAD_REQUEST);
                    return;
                }

                IKeyFrameDataAnalyzer.KeyFrameMeta keyFrameMeta = ((IKeyFrameDataAnalyzer) reader).analyzeKeyFrames();
                String s = prepareContent(keyFrameMeta, file);
                reader.close();
                BufFacade data = BufFacade.buffer(127).writeBytes(s.getBytes());
                setHeader(resp);
                commitResponse(req, resp, data);
            } else {
                sendError(req, resp, HTTPResponseStatus.NOT_FOUND);
            }
        } else {
            sendError(req, resp, HTTPResponseStatus.NOT_FOUND);
        }
    }

    String prepareContent(IKeyFrameDataAnalyzer.KeyFrameMeta keyFrameMeta, File file) {
        long[] positions = keyFrameMeta.positions;  //关键帧的字节文字
        int[] timestamps = keyFrameMeta.timestamps; //表示每个关键帧此时视频的时长


        int duration = ExtConfiguration.HLS_SEGMENT_TIME * 1000; //理论分片时间
        float fixDuration = 0; //分片时间修正值
        int nextTime = duration; //下一个timestamp 应该等于这个时间即可按理论时长间隔duration分片, 大于时修正duration 使为实际时长间隔


        StringBuilder sb = new StringBuilder();
        sb.append("#EXTM3U\n")
                .append("#EXT-X-VERSION:3\n")
                .append("#EXT-X-TARGETDURATION:").append(ExtConfiguration.HLS_SEGMENT_TIME).append("\n")
                .append("#EXT-X-MEDIA-SEQUENCE:1\n");

        int seqNum = 1;
        int rest = 0;  //代表是否最后执行的为else
        long startPos = positions[1];
        for (int i = 0; i < positions.length; i++) {
            if (positions[i] < positions[1]) {
                continue;
            }
            if (timestamps[i] >= nextTime) {
                rest = 0;  //执行符合条件的清零
                fixDuration = timestamps[i] - nextTime; //获取差值
                fixDuration = (duration + fixDuration) / 1000; //计算修正后的间隔时长


                sb.append("#EXTINF:").append(fixDuration).append(",\n");
                if (i == (positions.length - 1)) { //last one
                    sb.append(String.format("%s_%s_%d.ts?type=vod\n", startPos, file.length(), seqNum));
                    sb.append("\r\n");
                } else {
                    sb.append(String.format("%s_%s_%d.ts?type=vod\n", startPos, positions[i], seqNum));
                    sb.append("\r\n");
                }

                seqNum++;
                startPos = positions[i];
                nextTime = timestamps[i] + duration; // fix next time
            } else {
                rest++; //执行else的时候++
            }
        }

        // last time < duration
        if (rest > 0) {
            float lastOneDuration = (duration - (nextTime - timestamps[timestamps.length - 1])) / 1000;
            sb.append("#EXTINF:").append(lastOneDuration).append(",\n");
            sb.append(String.format("%s_%s_%d.ts?type=vod\n", startPos, file.length(), seqNum));
            sb.append("\r\n");
        }

        sb.append("#EXT-X-ENDLIST\n");
        return sb.toString();
    }


    @Override
    public void setHeader(HTTPResponse resp) {
        resp.addHeader("Accept-Ranges", "bytes");
        resp.addHeader(CONTENT_TYPE, "application/vnd.apple.mpegurl");
        resp.addHeader("Pragma", "no-cache");
        resp.setHeader("Cache-Control", "no-cache");
    }

}
