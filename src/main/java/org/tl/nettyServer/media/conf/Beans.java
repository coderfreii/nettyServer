package org.tl.nettyServer.media.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.tl.nettyServer.media.io.flv.impl.FLVService;
import org.tl.nettyServer.media.scheduling.ISchedulingService;
import org.tl.nettyServer.media.scheduling.JDKSchedulingService;
import org.tl.nettyServer.media.service.IStreamableFileService;
import org.tl.nettyServer.media.service.StreamableFileServiceFactory;
import org.tl.nettyServer.media.service.consumer.ConsumerService;
import org.tl.nettyServer.media.service.consumer.IConsumerService;
import org.tl.nettyServer.media.service.provider.IProviderService;
import org.tl.nettyServer.media.service.provider.ProviderService;
import org.tl.nettyServer.media.stream.client.ClientBroadcastStream;
import org.tl.nettyServer.media.stream.client.PlaylistSubscriberStream;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class Beans {

    @Bean(name = ISchedulingService.BEAN_NAME)
    public JDKSchedulingService schedulingService() {
        return new JDKSchedulingService();
    }


    @Bean(name = "streamableFileFactory")
    public StreamableFileServiceFactory streamableFileFactory() {
        StreamableFileServiceFactory factory = new StreamableFileServiceFactory();
        Set<IStreamableFileService> services = new HashSet<>();

        FLVService flv = new FLVService();
        flv.setGenerateMetadata(true);
        services.add(flv);
//        services.add(new MP3Service());
//        services.add(new MP4Service());
//        services.add(new M4AService());
        factory.setServices(services);
        return factory;
    }

    @Bean(name = "playlistSubscriberStream")  //一个连接一个
    @Lazy
    @Scope("prototype")
    public PlaylistSubscriberStream playlistSubscriberStream() {
        PlaylistSubscriberStream cache = new PlaylistSubscriberStream();
        cache.setBufferCheckInterval(ExtConfiguration.INTERVAL);
        cache.setUnderrunTrigger(ExtConfiguration.TRIGGER);
        cache.setRepeat(true);
        return cache;
    }

    @Bean(name = "clientBroadcastStream")
    @Lazy
    @Scope("prototype")
    public ClientBroadcastStream clientBroadcastStream() {
        ClientBroadcastStream cache = new ClientBroadcastStream();
        return cache;
    }


    @Bean(name = IConsumerService.KEY)
    public ConsumerService consumerService() {
        return new ConsumerService();
    }


    @Bean(name = IProviderService.BEAN_NAME)
    public ProviderService providerService() {
        return new ProviderService();
    }
}
