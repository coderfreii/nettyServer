import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.name.Rename;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.io.File;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;

public class Test {


    public static void main(String[] args) throws IOException {
        Thumbnails.of(new File("C:\\Users\\Administrator\\Desktop\\人脸识别\\人脸识别").listFiles())
                .size(640, 480)
                .outputFormat("jpg")
                .toFiles(Rename.PREFIX_DOT_THUMBNAIL);





    }


}
