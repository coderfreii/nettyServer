package org.tl.nettyServer.media.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.tl.nettyServer.media.io.flv.impl.FLVService;
import org.tl.nettyServer.media.scheduling.ISchedulingService;
import org.tl.nettyServer.media.scheduling.JDKSchedulingService;
import org.tl.nettyServer.media.service.IConsumerService;
import org.tl.nettyServer.media.service.IProviderService;
import org.tl.nettyServer.media.service.IStreamableFileService;
import org.tl.nettyServer.media.service.ProviderService;
import org.tl.nettyServer.media.stream.ConsumerService;
import org.tl.nettyServer.media.stream.PlaylistSubscriberStream;
import org.tl.nettyServer.media.stream.StreamableFileFactory;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class Beans {

    @Bean(name = ISchedulingService.BEAN_NAME)
    public JDKSchedulingService schedulingService() {
        return new JDKSchedulingService();
    }


    @Bean(name = "streamableFileFactory")
    public StreamableFileFactory streamableFileFactory() {
        StreamableFileFactory factory = new StreamableFileFactory();
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

    @Bean(name = "playlistSubscriberStream")
    @Lazy
    @Scope("prototype")
    public PlaylistSubscriberStream playlistSubscriberStream() {
        PlaylistSubscriberStream cache = new PlaylistSubscriberStream();
        cache.setBufferCheckInterval(ExtConfiguration.INTERVAL);
        cache.setUnderrunTrigger(ExtConfiguration.TRIGGER);
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
