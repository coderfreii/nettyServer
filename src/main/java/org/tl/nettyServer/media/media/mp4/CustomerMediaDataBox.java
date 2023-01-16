package org.tl.nettyServer.media.media.mp4;

import org.mp4parser.BoxParser;
import org.mp4parser.ParsableBox;
import org.mp4parser.boxes.iso14496.part12.MediaDataBox;
import org.mp4parser.support.DoNotParseDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import sun.nio.ch.FileChannelImpl;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;

public class CustomerMediaDataBox implements ParsableBox, Closeable {
    public static final String TYPE = "mdat";
    private static Logger LOG = LoggerFactory.getLogger(MediaDataBox.class);
    ByteBuffer header;
    File dataFile;

    public CustomerMediaDataBox() {
    }

    public String getType() {
        return "mdat";
    }

    public void getBox(WritableByteChannel writableByteChannel) throws IOException {
        writableByteChannel.write((ByteBuffer) this.header.rewind());
        FileChannel fc = (new FileInputStream(this.dataFile)).getChannel();
        fc.transferTo(0L, this.dataFile.lastModified(), writableByteChannel);
        fc.close();
    }

    public long getSize() {
        return (long) this.header.limit() + this.dataFile.length();
    }

    @DoNotParseDetail
    public void parse(ReadableByteChannel dataSource, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {
        FileChannelImpl fci = (FileChannelImpl) dataSource;
        Field path = ReflectionUtils.findField(fci.getClass(), "path");
        boolean accessible = path.isAccessible();
        path.setAccessible(true);
        String field = (String) ReflectionUtils.getField(path, fci);
        path.setAccessible(accessible);
        this.dataFile = new File(field);
        this.header = ByteBuffer.allocate(header.limit());
        this.header.put(header);
    }

    public void close() throws IOException {
        try {
            Files.delete(this.dataFile.toPath());
        } catch (IOException var2) {
            LOG.warn("failed to delete: " + this.dataFile.getAbsolutePath() + ". I'll try to delete it on exit.", var2);
            this.dataFile.deleteOnExit();
        }
    }
}
