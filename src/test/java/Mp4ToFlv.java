import org.tl.nettyServer.media.io.ITag;
import org.tl.nettyServer.media.media.flv.impl.FLVWriter;
import org.tl.nettyServer.media.media.mp4.MP4Reader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Mp4ToFlv {
    public static void main(String[] args) throws IOException {
        File in = new File("D:\\tl\\webapps\\oflaDemo\\streams\\7.mp4");
        File out = new File("D:\\tl\\webapps\\oflaDemo\\streams\\7.flv");
        MP4Reader mp4Reader = new MP4Reader(in);


        FLVWriter writer = new FLVWriter(out, false);
        while (mp4Reader.hasMoreTags()) {
            ITag iTag = mp4Reader.readTag();
            if (iTag != null) {
                writer.writeTag(iTag);
            }
        }

        mp4Reader.close();
        writer.close();
    }
}
