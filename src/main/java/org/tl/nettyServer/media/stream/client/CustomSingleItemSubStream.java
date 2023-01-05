package org.tl.nettyServer.media.stream.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.client.IContext;
import org.tl.nettyServer.media.codec.IAudioStreamCodec;
import org.tl.nettyServer.media.codec.IVideoStreamCodec;
import org.tl.nettyServer.media.exception.StreamNotFoundException;
import org.tl.nettyServer.media.io.IStreamableFile;
import org.tl.nettyServer.media.io.ITag;
import org.tl.nettyServer.media.io.ITagReader;
import org.tl.nettyServer.media.io.flv.FLVUtils;
import org.tl.nettyServer.media.messaging.IMessageOutput;
import org.tl.nettyServer.media.messaging.IPipe;
import org.tl.nettyServer.media.messaging.InMemoryPushPushPipe;
import org.tl.nettyServer.media.net.rtmp.codec.AudioCodec;
import org.tl.nettyServer.media.net.rtmp.codec.VideoCodec;
import org.tl.nettyServer.media.scheduling.ISchedulingService;
import org.tl.nettyServer.media.scope.IBasicScope;
import org.tl.nettyServer.media.scope.IBroadcastScope;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.scope.ScopeType;
import org.tl.nettyServer.media.service.IStreamableFileService;
import org.tl.nettyServer.media.service.IStreamableFileServiceFactory;
import org.tl.nettyServer.media.service.StreamableFileServiceFactory;
import org.tl.nettyServer.media.service.consumer.IConsumerService;
import org.tl.nettyServer.media.service.provider.IProviderService;
import org.tl.nettyServer.media.stream.StreamState;
import org.tl.nettyServer.media.stream.base.IClientStream;
import org.tl.nettyServer.media.stream.codec.IStreamCodecInfo;
import org.tl.nettyServer.media.stream.conn.IStreamCapableConnection;
import org.tl.nettyServer.media.stream.consumer.ICustomPushableConsumer;
import org.tl.nettyServer.media.stream.engin.PlayEngine;
import org.tl.nettyServer.media.stream.playlist.IPlayItem;
import org.tl.nettyServer.media.util.ScopeUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class CustomSingleItemSubStream extends SingleItemSubscriberStream {

    private static final Logger log = LoggerFactory.getLogger(CustomSingleItemSubStream.class);

    private ICustomPushableConsumer consumer;
    private IProviderService providerService;
    private IPlayItem item;
    private boolean isFailure;
    private boolean isLive;
    /**
     * Service used to provide notifications, keep client buffer filled, clean up, etc...
     */
    protected ISchedulingService schedulingService;

    public CustomSingleItemSubStream(IScope scope, final ICustomPushableConsumer consumer) {

        this.setScope(scope);
        this.consumer = consumer;
        this.setClientBufferDuration(2000);
        this.setConnection((IStreamCapableConnection) consumer.getConnection());
        Red5.setConnectionLocal(consumer.getConnection());
    }

    @Override
    public IStreamCapableConnection getConnection() {
        return (IStreamCapableConnection) consumer.getConnection();
    }

    @Override
    public void start() {

        // ensure the play engine exists
        if (engine == null) {
            IScope scope = getScope();
            if (scope != null) {

                IContext ctx = scope.getContext();
                if (ctx.hasBean(ISchedulingService.BEAN_NAME)) {
                    schedulingService = (ISchedulingService) ctx.getBean(ISchedulingService.BEAN_NAME);
                } else {
                    //try the parent
                    schedulingService = (ISchedulingService) scope.getParent().getContext().getBean(ISchedulingService.BEAN_NAME);
                }

                //此处和 为啥没有从上下文提取
                IConsumerService consumerService = new IConsumerService() {
                    @Override
                    public IMessageOutput getConsumerOutput(IClientStream stream) {
                        IPipe pipe = new InMemoryPushPushPipe();
                        pipe.subscribe(consumer, null);
                        return pipe;
                    }

                };

                if (ctx.hasBean(IProviderService.BEAN_NAME)) {
                    providerService = (IProviderService) ctx.getBean(IProviderService.BEAN_NAME);
                } else {
                    //try the parent
                    providerService = (IProviderService) scope.getParent().getContext().getBean(IProviderService.BEAN_NAME);
                }
                engine = new PlayEngine.Builder(this, schedulingService, consumerService, providerService).build();
            }
        }
        // set buffer check interval
        engine.setBufferCheckInterval(1000);
        // set underrun trigger
        engine.setUnderRunTrigger(5000);
        engine.setMaxPendingVideoFrames(2000);
        // Start playback engine
        engine.start();
        isFailure = false;
    }

    @Override
    public void play() throws IOException {
        try {
            engine.play(item);
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
            isFailure = true;
        } catch (StreamNotFoundException e) {
            log.info(e.getMessage());
            isFailure = true;
        }
    }

    @Override
    public void close() {

        if (state.get() != StreamState.CLOSED) {
            super.close();
        }
    }

    @Override
    public void onChange(StreamState state, Object... changed) {

        super.onChange(state, changed);
        if (state == StreamState.STOPPED) {
            consumer.getConnection().close();
        } else if (state == StreamState.PLAYING) {
            isLive = (Boolean) changed[1];
        }
    }

    @Override
    public void setPlayItem(IPlayItem item) {
        this.item = item;
        super.setPlayItem(item);
    }

    public boolean isFailure() {
        return isFailure;
    }

    public ICustomPushableConsumer getConsumer() {
        return consumer;
    }

    public boolean isLive() {

        return isLive;
    }

    public int getLastPlayTs() {

        int ts = engine.getLastMessageTimestamp();
        return ts;
    }

    public IProviderService.INPUT_TYPE lookupStreamInput() {
        IScope scope = getScope();
        /*IProviderService providerService = (IProviderService) scope.getContext().getBean(ScopeContextBean.PROVIDERSERVICE_BEAN);*/
        return providerService.lookupProviderInput(scope, item.getName(), 0);
    }

    /**
     * @param videoConfig
     * @param audioConfig
     * @return
     * @throws IOException
     */
    public void getConfig(BufFacade videoConfig, BufFacade audioConfig, AtomicLong duration) throws IOException {


        IScope scope = getScope();
        /*IProviderService providerService = (IProviderService) scope.getContext().getBean(ScopeContextBean.PROVIDERSERVICE_BEAN);*/
        IProviderService.INPUT_TYPE result = lookupStreamInput();

        if (result == IProviderService.INPUT_TYPE.VOD) { // reader file get video and audio config
            File file = providerService.getVODProviderFile(scope, item.getName());
            if (file != null && file.exists()) {
                /*IStreamableFileFactory factory = StreamableFileFactory.getInstance();*/
                IStreamableFileServiceFactory factory = (IStreamableFileServiceFactory) ScopeUtils.getScopeService(scope, IStreamableFileServiceFactory.class, StreamableFileServiceFactory.class);

                IStreamableFileService service = factory.getService(file);
                boolean audioChecked = false;
                boolean videoChecked = false;
                IStreamableFile streamFile = service.getStreamableFile(file);
                ITagReader reader = streamFile.getReader();
                duration.set(reader.getDuration());
                ITag tag;
                int codec;
                for (int i = 0; i < 10; i++) {
                    if (audioChecked && videoChecked) break;
                    tag = reader.readTag();
                    if (tag == null) return;
                    if (ITag.TYPE_VIDEO == tag.getDataType()) {
                        codec = FLVUtils.getVideoCodec(tag.getBody().getByte(0));
                        if (codec == VideoCodec.AVC.getId() && tag.getBody().getByte(1) == 0x00) {
                            videoChecked = true;
                            videoConfig.writeBytes(tag.getBody());
                        }
                    } else if (ITag.TYPE_AUDIO == tag.getDataType()) {
                        codec = FLVUtils.getAudioCodec(tag.getBody().getByte(0));
                        if ((codec == AudioCodec.AAC.getId() && tag.getBody().getByte(1) == 0x00) || codec == AudioCodec.MP3.getId()) {
                            audioChecked = true;
                            audioConfig.writeBytes(tag.getBody());
                        }
                    }
                }
                reader.close();
            }
        } else if (result == IProviderService.INPUT_TYPE.LIVE) { // get live video and audio config
            //IBasicScope basicScope = scope.getBasicScope(ScopeType.APPLICATION, item.getName());
            //IBasicScope basicScope1 = scope.getBasicScope(ScopeType.GLOBAL, item.getName());
            IBasicScope basicScope = scope.getBasicScope(ScopeType.BROADCAST, item.getName());


            IClientBroadcastStream bs = null;
            if (basicScope != null && basicScope instanceof IBroadcastScope) {
                IBroadcastScope bss = (IBroadcastScope) basicScope;
                bs = (IClientBroadcastStream) bss.getClientBroadcastStream();
            }

            if (bs != null) {
                IStreamCodecInfo codecInfo = bs.getCodecInfo();
                IVideoStreamCodec videoCodecInfo = null;
                IAudioStreamCodec audioCodecInfo = null;
                if (codecInfo != null) {
                    videoCodecInfo = codecInfo.getVideoCodec();
                    audioCodecInfo = codecInfo.getAudioCodec();
                }

                if (videoCodecInfo != null && videoCodecInfo.getDecoderConfiguration() != null) {
                    videoConfig.writeBytes(videoCodecInfo.getDecoderConfiguration());
                }

                if (audioCodecInfo != null && audioCodecInfo.getDecoderConfiguration() != null) {
                    audioConfig.writeBytes(audioCodecInfo.getDecoderConfiguration());
                }
            }
        }
    }

    public IPlayItem getPlayItem() {
        return item;
    }
}
