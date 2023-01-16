import org.tl.nettyServer.media.io.ITag;
import org.tl.nettyServer.media.io.ITagReader;
import org.tl.nettyServer.media.media.flv.impl.FLV;
import org.tl.nettyServer.media.media.flv.impl.FLVWriter;

import java.io.File;
import java.io.IOException;

public class FLVToFile {


    public static void main(String[] args) throws IOException {
        File in = new File("D:\\tl\\webapps\\oflaDemo\\streams\\6.flv");
        File out = new File("D:\\tl\\webapps\\oflaDemo\\streams\\test.flv");
        FLV flv = new FLV(in, false);
        ITagReader reader = flv.getReader();
        FLVWriter flvWriter = new FLVWriter(out, false);
        try {
            while (reader.hasMoreTags()) {
                ITag iTag = reader.readTag();
                if (iTag != null) {
                    flvWriter.writeTag(iTag);
                }
            }
        } catch (RuntimeException e) {
            System.out.println();
        } finally {
            reader.close();
            flvWriter.close();
        }
    }
}
