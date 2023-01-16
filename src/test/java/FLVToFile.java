import org.tl.nettyServer.media.io.ITag;
import org.tl.nettyServer.media.media.flv.impl.FLVReader;
import org.tl.nettyServer.media.media.flv.impl.FLVWriter;

import java.io.File;
import java.io.IOException;

public class FLVToFile {


    public static void main(String[] args) throws IOException {
        FLVReader flvReader = new FLVReader(new File("D:\\tl\\webapps\\oflaDemo\\streams\\6.flv"));
        FLVWriter flvWriter = new FLVWriter(new File("D:\\tl\\test.flv"), false);
        try {
            if (flvReader.hasMoreTags()) {
                ITag iTag = flvReader.readTag();
                if (iTag != null) {
                    flvWriter.writeTag(iTag);
                }
            }
        } finally {
            flvReader.close();
            flvWriter.close();
        }
    }
}
