package org.tl.nettyServer.media.net.hls.service;


import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.cache.impl.CacheManager;
import org.tl.nettyServer.media.cache.impl.ObjectCache;
import org.tl.nettyServer.media.client.ScopeContextBean;
import org.tl.nettyServer.media.io.IStreamableFile;
import org.tl.nettyServer.media.io.ITag;
import org.tl.nettyServer.media.io.ITagReader;
import org.tl.nettyServer.media.media.flv.FLVUtils;
import org.tl.nettyServer.media.net.hls.ts.codec.FLV2MPEGTSChunkWriter;
import org.tl.nettyServer.media.net.hls.ts.message.MpegTsSegment;
import org.tl.nettyServer.media.net.hls.ts.service.MpegTsSegmentService;
import org.tl.nettyServer.media.net.http.codec.QueryStringDecoder;
import org.tl.nettyServer.media.net.http.conn.HTTPConnection;
import org.tl.nettyServer.media.net.http.message.DefaultHttpChunk;
import org.tl.nettyServer.media.net.http.message.DefaultHttpChunkTrailer;
import org.tl.nettyServer.media.net.http.message.HTTPHeaders;
import org.tl.nettyServer.media.net.http.request.HTTPRequest;
import org.tl.nettyServer.media.net.http.response.HTTPResponse;
import org.tl.nettyServer.media.net.http.response.HTTPResponseStatus;
import org.tl.nettyServer.media.net.http.service.BaseHTTPService;
import org.tl.nettyServer.media.net.http.service.IHTTPService;
import org.tl.nettyServer.media.net.rtmp.codec.AudioCodec;
import org.tl.nettyServer.media.net.rtmp.codec.VideoCodec;
import org.tl.nettyServer.media.net.rtmp.event.AudioData;
import org.tl.nettyServer.media.net.rtmp.event.VideoData;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.service.IStreamableFileService;
import org.tl.nettyServer.media.service.IStreamableFileServiceFactory;
import org.tl.nettyServer.media.service.StreamableFileServiceFactory;
import org.tl.nettyServer.media.service.provider.IProviderService;
import org.tl.nettyServer.media.util.ScopeUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * HTTP Live Stream Mpegts Service
 *
 * @author pengliren
 */
@Slf4j
public class HTTPTSService extends BaseHTTPService implements IHTTPService {

    private static final Pattern TS_PATTERN = Pattern.compile("(\\d+)_(\\d+)_(\\d+)\\.ts$");

    private static ObjectCache fileCache;

    @Override
    public void handleRequest(HTTPRequest req, HTTPResponse resp, IScope scope) throws Exception {

        String method = req.getMethod().toString();
        if (!REQUEST_GET_METHOD.equalsIgnoreCase(method) && !REQUEST_POST_METHOD.equalsIgnoreCase(method)) {
            // Bad request - return simple error page
            sendError(req, resp, HTTPResponseStatus.BAD_REQUEST);
            return;
        }

        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.getPath());
        String path = queryStringDecoder.getPath().substring(1);
        String[] segments = path.split("/");
        String app = scope.getName();

        if (segments.length < 2) {
            sendError(req, resp, HTTPResponseStatus.BAD_REQUEST);
            return;
        }

        String streamName;
        String tsIndex;
        streamName = segments[0];
        tsIndex = segments[1];

        Map<String, List<String>> params = queryStringDecoder.getParameters();
        String type = "";
        if (params.get("type") != null && params.get("type").size() > 0) {
            type = params.get("type").get(0);
        }

        if (type.equals("live")) { // live
            playLiveTsStream(scope, app, streamName, tsIndex, req, resp);
        } else if (type.equals("vod")) { // vod
            playVodTsStream(scope, app, streamName, tsIndex, req, resp);
        } else { // no found
            sendError(req, resp, HTTPResponseStatus.NOT_FOUND);
        }
    }

    private void playLiveTsStream(IScope scope, String app, String streamName, String tsIndex, HTTPRequest req, HTTPResponse resp) {

        tsIndex = tsIndex.substring(0, tsIndex.lastIndexOf(".ts"));
        int sequenceNumber = Integer.valueOf(tsIndex);
        MpegTsSegmentService service = MpegTsSegmentService.getInstance();
        if (service.isAvailable(scope, streamName)) {
            MpegTsSegment segment = service.getSegment(app, streamName, sequenceNumber);
            if (segment != null && segment.isClosed()) {
                BufFacade data = segment.getBuffer().asReadOnly();
                setHeader(resp);
                commitResponse(req, resp, data);
            } else {
                sendError(req, resp, HTTPResponseStatus.NOT_FOUND);
            }
        }
    }

    private String generateCacheKey(String app, String streamName, String what) {
        String colon = ":";
        return app + colon + streamName + what;
    }

    private void playVodTsStream(IScope scope, String app, String streamName, String tsIndex, HTTPRequest req, HTTPResponse resp) {
        Matcher m = TS_PATTERN.matcher(tsIndex);
        int start;
        int end;
        if (m.matches()) {
            start = Integer.valueOf(m.group(1));
            end = Integer.valueOf(m.group(2));
        } else {
            sendError(req, resp, HTTPResponseStatus.BAD_REQUEST);
            return;
        }

        HTTPConnection conn = (HTTPConnection) Red5.getConnectionLocal();
        IProviderService providerService = (IProviderService) scope.getContext().getBean(ScopeContextBean.PROVIDERSERVICE_BEAN);
        File file = providerService.getVODProviderFile(scope, streamName);
        IStreamableFileServiceFactory factory = (IStreamableFileServiceFactory) ScopeUtils.getScopeService(scope, IStreamableFileServiceFactory.class, StreamableFileServiceFactory.class);
        IStreamableFileService service = factory.getService(file);
        if (service != null) {
            BufFacade data = BufFacade.buffer(4096);
            FLV2MPEGTSChunkWriter writer;


            BufFacade videoConfig = null;
            BufFacade audioConfig = null;
            List<VideoData> videos = new ArrayList<>();
            List<AudioData> audios = new ArrayList<>();
            try {
                ITagReader reader;
                try {
                    IStreamableFile streamFile = service.getStreamableFile(file);
                    reader = streamFile.getReader();
                } catch (IOException e) {
                    log.info("play hls exception {}", e.getMessage());
                    sendError(req, resp, HTTPResponseStatus.BAD_REQUEST);
                    return;
                }

                String videoConfigCacheKey = generateCacheKey(app, streamName, "videoConfig");
                String audioConfigCacheKey = generateCacheKey(app, streamName, "audioConfig");

                byte[] videoConfigBytes = (byte[]) getFileCache().get(videoConfigCacheKey);
                byte[] audioConfigBytes = (byte[]) getFileCache().get(audioConfigCacheKey);

                BufFacade storeVC = null;
                BufFacade storeAC = null;

                if (videoConfigBytes != null) {
                    storeVC = BufFacade.buffer(videoConfigBytes.length).writeBytes(videoConfigBytes);
                    storeVC.readerIndex(2);
                }
                if (videoConfigBytes != null) {
                    storeAC = BufFacade.buffer(audioConfigBytes.length).writeBytes(audioConfigBytes);
                    storeAC.readerIndex(2);
                }


                videoConfig = storeVC;
                audioConfig = storeAC;

                if (start > 0 && (videoConfig == null || audioConfig == null)) {  //TODO: in some case we dont have both for sure  how do we know ?
                    boolean audioChecked = false;
                    boolean videoChecked = false;
                    ITag tag;
                    for (int i = 0; i < 10; i++) {  // give ten time chance to find config
                        if (audioChecked && videoChecked) break;
                        tag = reader.readTag();
                        if (tag == null) return;
                        if (ITag.TYPE_VIDEO == tag.getDataType()) {
                            videoChecked = true;
                            if (FLVUtils.getVideoCodec(tag.getBody().readByte()) == VideoCodec.AVC.getId() && tag.getBody().readByte() == 0x00) {
                                videoConfig = tag.getBody();
                                getFileCache().put(videoConfigCacheKey, videoConfig.array());
                            }
                        } else if (ITag.TYPE_AUDIO == tag.getDataType()) {
                            audioChecked = true;
                            if (FLVUtils.getAudioCodec(tag.getBody().readByte()) == AudioCodec.AAC.getId() && tag.getBody().readByte() == 0x00) {
                                audioConfig = tag.getBody();
                                getFileCache().put(audioConfigCacheKey, audioConfig.array());
                            }
                        }
                    }
                } else {
                    //tag.getDataType() 为18 的第一次才发送metadata  需要正确设置标志位
                    reader.setMetaSent(true);
                }

                reader.position(start);

                writer = new FLV2MPEGTSChunkWriter(videoConfig, audioConfig, false);

                writer.startChunkTS(data);

                reader.position(start);
                while (reader.hasMoreTags()) {
                    if (conn.isClosing()) return;// if client conn is close we must stop release resources
                    if (end != -1 && reader.getBytesRead() + 4 >= end) break;  //读到分片结束

                    ITag tag = reader.readTag();
                    if (tag == null) break; // fix tag NPE

                    if (tag.getDataType() == ITag.TYPE_VIDEO) {
                        VideoData videoData = new VideoData(tag.getBody());
                        videoData.setTimestamp(tag.getTimestamp());
                        writer.writeStreamEvent(videoData);
                        videos.add(videoData);

                    } else if (tag.getDataType() == ITag.TYPE_AUDIO) {
                        AudioData audioData = new AudioData(tag.getBody());
                        audioData.setTimestamp(tag.getTimestamp());
                        writer.writeStreamEvent(audioData);
                        audios.add(audioData);
                    }
                }
                reader.close();
            } catch (IOException e) {
                log.info("play vod exception {}", e.getMessage());
                sendError(req, resp, HTTPResponseStatus.BAD_REQUEST);
                return;
            }

            writer.endChunkTS();
            setHeader(resp);


            String appScopeName = ScopeUtils.findApplication(scope).getName();
            String createFile = String.format("%s/webapps/%s/hls/%s/%s", System.getProperty("red5.root"), appScopeName, streamName, tsIndex);
            File createF = new File(createFile);

            if (!createF.exists()) {
                try {
                    createF.createNewFile();
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(createFile);
                        data.markReaderIndex();
                        byte[] cc = new byte[data.readableBytes()];
                        data.readBytes(cc);
                        data.resetReaderIndex();
                        fileOutputStream.write(cc);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            resp.addHeader(HTTPHeaders.Names.CONNECTION, HTTPHeaders.Values.KEEP_ALIVE);
            resp.addHeader(HTTPHeaders.Names.CONTENT_LENGTH, data.readableBytes());
            commitResponse(req, resp, null);

            conn.write(new DefaultHttpChunk(data));
            conn.write(new DefaultHttpChunkTrailer());
        } else {
            sendError(req, resp, HTTPResponseStatus.NOT_FOUND);
        }
    }

    public static ObjectCache getFileCache() {
        if (fileCache == null) {
            fileCache = CacheManager.getInstance().getCache("org.red5.server.stream.hls.fileCache");
        }
        return fileCache;
    }

    @Override
    public void setHeader(HTTPResponse resp) {
//        resp.addHeader("Accept-Ranges", "bytes");
//        resp.addHeader(CONTENT_TYPE, "video/MP2T");
        resp.addHeader(HTTPHeaders.Names.TRANSFER_ENCODING, HTTPHeaders.Values.CHUNKED);
        resp.addHeader("Pragma", "no-cache");
        resp.setHeader("Cache-Control", "no-cache");
    }

}
