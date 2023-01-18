package org.tl.nettyServer.media;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

@SpringBootApplication
public class Starter {
    public static void main(String[] args) {
        // 获取mxbean
        for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (!(mbean instanceof NotificationEmitter)) {
                continue;//假如不支持监听...
            }
            final NotificationEmitter emitter = (NotificationEmitter) mbean;//添加监听
            //  final NotificationListener listener = getNewListener(mbean);
            final NotificationListener listener = new GarbageNotificationListener();
            emitter.addNotificationListener(listener, null, null);
        }
        SpringApplication.run(Starter.class, args);
        HttpServer.main(args);
        RtspServer.main(args);
        WsServer.main(args);
        RtmpServer.main(args);
    }

    // 监听器 需要实现NotificationListener
    static class GarbageNotificationListener implements NotificationListener {
        @Override
        public void handleNotification(Notification notification, Object handback) {
            String notifType = notification.getType();
            if (notifType.equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                CompositeData cd = (CompositeData) notification.getUserData();
                GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from(cd);
                StringBuilder stringBuilder = new StringBuilder();
                GcInfo gcInfo = info.getGcInfo();
                stringBuilder.append(String.format("id: %s", gcInfo.getId()));
                stringBuilder.append("\r\n");
                stringBuilder.append(String.format("GcName: %s", info.getGcName()));
                stringBuilder.append("\r\n");
                stringBuilder.append(String.format("GcAction: %s", info.getGcAction()));
                stringBuilder.append("\r\n");
                stringBuilder.append(String.format("GcCause: %s", info.getGcCause()));
                stringBuilder.append("\r\n");
                stringBuilder.append(String.format("duration: %s", gcInfo.getDuration()));
                stringBuilder.append("\r\n");
                stringBuilder.append(String.format("UsageBeforeGc: %s", gcInfo.getMemoryUsageBeforeGc()));
                stringBuilder.append("\r\n");
                stringBuilder.append(String.format("UsageAfterGc: %s", gcInfo.getMemoryUsageAfterGc()));
                System.out.println(stringBuilder);
            }
        }
    }
}
