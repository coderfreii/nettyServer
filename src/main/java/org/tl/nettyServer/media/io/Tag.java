

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

package org.tl.nettyServer.media.io;

import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.buf.ReleaseUtil;

/**
 * A Tag represents the contents or payload of a FLV file.
 *
 * @author The Red5 Project
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @see <a href="https://code.google.com/p/red5/wiki/FLV#FLV_Tag">FLV Tag</a>
 */
public class Tag implements ITag {

    private byte type;

    private byte dataType;

    private int timestamp;

    private int bodySize;

    private BufFacade body;

    private int previousTagSize;

    private byte bitflags;

    public Tag(byte dataType, int timestamp, int bodySize, BufFacade body, int previousTagSize) {
        this.dataType = dataType;
        this.timestamp = timestamp;
        this.bodySize = bodySize;
        this.body = body;
        this.previousTagSize = previousTagSize;
    }

    public Tag() {
    }

    public byte getBitflags() {
        return bitflags;
    }

    public void setBitflags(byte bitflags) {
        this.bitflags = bitflags;
    }

    @Override
    public BufFacade getData() {
        return null;
    }

    @Override
    public BufFacade getBody() {
        return body;
    }

    @Override
    public int getBodySize() {
        return bodySize;
    }

    @Override
    public byte getDataType() {
        return dataType;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public int getPreviousTagSize() {
        return previousTagSize;
    }

    @Override
    public String toString() {
        String ret = "Data Type\t=" + dataType + "\n";
        ret += "Prev. Tag Size\t=" + previousTagSize + "\n";
        ret += "Body size\t=" + bodySize + "\n";
        ret += "timestamp\t=" + timestamp + "\n";
        ret += "Body Data\t=" + body + "\n";
        return ret;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    @Override
    public void setBody(BufFacade body) {
        this.body = body;
    }

    @Override
    public void setBodySize(int bodySize) {
        this.bodySize = bodySize;
    }

    @Override
    public void setDataType(byte dataType) {
        this.dataType = dataType;
    }

    @Override
    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public void setData() {
    }

    @Override
    public void setPreviousTagSize(int size) {
        this.previousTagSize = size;
    }

    @Override
    public boolean release() {
        boolean b = true;
        boolean d = true;

        if (this.getBody() != null) {
            b = this.getBody().release();
        }

        if (this.getData() != null) {
            d = ReleaseUtil.release(this.getData());
        }


        if (b && d) {
            return true;
        } else {
            return false;
        }
    }
}
