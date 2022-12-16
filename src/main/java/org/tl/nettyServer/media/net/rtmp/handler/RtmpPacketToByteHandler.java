package org.tl.nettyServer.media.net.rtmp.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.extern.slf4j.Slf4j;
import org.tl.nettyServer.media.Red5;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtmp.codec.RTMPProtocolEncoder;
import org.tl.nettyServer.media.net.rtmp.conn.RTMPConnection;
import org.tl.nettyServer.media.net.rtmp.message.Packet;
import org.tl.nettyServer.media.session.SessionAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

@Slf4j
public class RtmpPacketToByteHandler extends MessageToMessageEncoder<Packet> {
    private RTMPProtocolEncoder rtmpProtocolEncoder = new RTMPProtocolEncoder();

    private int targetChunkSize = 2048;


    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, List<Object> out) throws Exception {
        RTMPConnection conn = (RTMPConnection) SessionAccessor.resolveConn(ctx);
        RTMPConnection localConn = (RTMPConnection) Red5.getConnectionLocal();
        if (conn != null) {
            //TODO
            if (!conn.equals(localConn)) {
                if (localConn != null) {
                    log.debug("Connection local ({}) didn't match io session ({})", localConn.getSessionId(), conn.getSessionId());
                }
                // replace conn with the one from the session id lookup
                Red5.setConnectionLocal(conn);
            }

            Boolean interrupted = false;
            Semaphore lock = conn.getEncoderLock();

            try {
                lock.acquire();
                log.trace("Encoder lock acquired {}", conn.getSessionId());
                //将message的信息全部写进buf
                BufFacade buf = rtmpProtocolEncoder.encode(msg);
                if (buf != null) {
                    int requestedWriteChunkSize = conn.getState().getWriteChunkSize();
                    log.trace("Requested chunk size: {} target chunk size: {}", requestedWriteChunkSize, targetChunkSize);
                    if (buf.readableBytes() <= targetChunkSize * 2) {
                        log.trace("Writing output data");
                        out.add(buf);
                    } else {
                        //过大则分开发送
                        int sentChunks = Chunk.chunkAndWrite(out, buf, requestedWriteChunkSize, targetChunkSize);
                        log.trace("Wrote {} chunks", sentChunks);
                    }
                } else {
                    log.trace("Response buffer was null after encoding");
                }
            } finally {
                lock.release();
                if (interrupted && log.isInfoEnabled()) {
                    log.info("Released lock after interruption. session {}, permits {}", conn.getSessionId(), lock.availablePermits());
                }
            }

            //TODO
            // set connection local back to previous value
            if (localConn != null) {
                Red5.setConnectionLocal(localConn);
            }
        }
    }


    /**
     * Output data chunker.
     */
    private static final class Chunk {

        @SuppressWarnings("unused")
        public static List<BufFacade> chunk(BufFacade message, int chunkSize, int desiredSize) {
            List<BufFacade> chunks = new ArrayList<BufFacade>();
            int targetSize = desiredSize > chunkSize ? desiredSize : chunkSize;
            int limit = message.readableBytes();
            do {
                int length = 0;
                int pos = message.readerIndex();
                while (length < targetSize && pos < limit) {
                    byte basicHeader = message.getByte(pos);
                    length += getDataSize(basicHeader) + chunkSize;
                    pos += length;
                }
                int remaining = message.readableBytes();
                log.trace("Length: {} remaining: {} pos+len: {} limit: {}", new Object[]{length, remaining, (message.readerIndex() + length), limit});
                if (length > remaining) {
                    length = remaining;
                }
                // add a chunk
                chunks.add(message.slice(0, length));
            } while (message.readable());
            return chunks;
        }

        /**
         * message 分割写入chunk
         *
         * @param out         出
         * @param message     消息
         * @param chunkSize   块大小
         * @param desiredSize 想要尺寸
         * @return int
         */
        public static int chunkAndWrite(List<Object> out, BufFacade message, int chunkSize, int desiredSize) {
            int sentChunks = 0;
            int targetSize = desiredSize > chunkSize ? desiredSize : chunkSize;
            int limit = message.readableBytes();
            do {
                int length = 0;
                int pos = message.readerIndex();
                while (length < targetSize && pos < limit) {
                    byte basicHeader = message.getByte(pos);
                    length += getDataSize(basicHeader) + chunkSize;
                    pos += length;
                }
                int remaining = message.readableBytes();
                log.trace("Length: {} remaining: {} pos+len: {} limit: {}", new Object[]{length, remaining, (message.readerIndex() + length), limit});
                if (length > remaining) {
                    length = remaining;
                }
                // send it
                BufFacade<Object> buffer = BufFacade.buffer(length);
                message.readBytes(buffer);
                out.add(buffer);
                sentChunks++;
            } while (message.readable());
            message.release();
            return sentChunks;
        }

        private static int getDataSize(byte basicHeader) {
            final int streamId = basicHeader & 0x0000003F;
            final int headerType = (basicHeader >> 6) & 0x00000003;
            int size = 0;
            switch (headerType) {
                case 0:
                    size = 12;
                    break;
                case 1:
                    size = 8;
                    break;
                case 2:
                    size = 4;
                    break;
                default:
                    size = 1;
                    break;
            }
            if (streamId == 0) {
                size += 1;
            } else if (streamId == 1) {
                size += 2;
            }
            return size;
        }
    }
}
