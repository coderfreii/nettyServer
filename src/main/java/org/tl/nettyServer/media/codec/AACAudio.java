/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 *
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tl.nettyServer.media.codec;


import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.net.rtmp.codec.AudioCodec;

/**
 * Red5 audio codec for the AAC audio format.
 * <p>
 * Stores the decoder configuration
 *
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Wittawas Nakkasem (vittee@hotmail.com)
 * @author Vladimir Hmelyoff (vlhm@splitmedialabs.com)
 */
public class AACAudio implements IAudioStreamCodec {

    private static Logger log = LoggerFactory.getLogger(AACAudio.class);

    public static final int[] AAC_SAMPLERATES = {96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350};

    /**
     * AAC audio codec constant
     */
    static final String CODEC_NAME = "AAC";

    /**
     * Block of data (AAC DecoderConfigurationRecord)
     */
    private byte[] blockDataAACDCR;

    /**
     * Constructs a new AACAudio
     */
    public AACAudio() {
        this.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return CODEC_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        blockDataAACDCR = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandleData(BufFacade data) {
        if (data.readableBytes() == 0) {
            // Empty buffer
            return false;
        }
        data.markReaderIndex();
        byte first = data.readByte();
        boolean result = (((first & 0xf0) >> 4) == AudioCodec.AAC.getId());
        data.resetReaderIndex();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addData(BufFacade data) {
        if (data.readable()) {
            // mark
            int start = data.readerIndex();
            // ensure we are at the beginning
            data.readerIndex(0);
            byte frameType = data.readByte();
            log.trace("Frame type: {}", frameType);
            byte header = data.readByte();
            // if we don't have the AACDecoderConfigurationRecord stored
            if (blockDataAACDCR == null) {
                if ((((frameType & 0xf0) >> 4) == AudioCodec.AAC.getId()) && (header == 0)) {
                    // back to the beginning
                    data.readerIndex(0);
                    blockDataAACDCR = new byte[data.readableBytes()];
                    data.readBytes(blockDataAACDCR);
                }
            }
            data.readerIndex(start);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufFacade getDecoderConfiguration() {
        if (blockDataAACDCR == null) {
            return null;
        }
        BufFacade result = BufFacade.buffer(blockDataAACDCR.length);
        result.writeBytes(blockDataAACDCR);
        result.readerIndex(0);
        return result;
    }

    @SuppressWarnings("unused")
    private static long sample2TC(long time, int sampleRate) {
        return (time * 1000L / sampleRate);
    }

    //private final byte[] getAACSpecificConfig() {		
    //	byte[] b = new byte[] { 
    //			(byte) (0x10 | /*((profile > 2) ? 2 : profile << 3) | */((sampleRateIndex >> 1) & 0x03)),
    //			(byte) (((sampleRateIndex & 0x01) << 7) | ((channels & 0x0F) << 3))
    //		};
    //	log.debug("SpecificAudioConfig {}", HexDump.toHexString(b));
    //	return b;	
    //}    
}
