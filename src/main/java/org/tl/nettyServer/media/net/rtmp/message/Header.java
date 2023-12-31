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

package org.tl.nettyServer.media.net.rtmp.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * RtmpProtocolState packet header    chunk header and message header
 */
public class Header implements Constants, Cloneable, Externalizable {

    private static final long serialVersionUID = 8982665579411495026L;


    private int csId;

    private int timerBase;

    private int timerDelta;

    private int dataSize;

    private byte dataType;

    private Number streamId = 0.0d;

    private boolean extended;

    public int getCsId() {
        return csId;
    }

    public void setCsId(int csId) {
        this.csId = csId;
    }

    public byte getDataType() {
        return dataType;
    }

    public void setDataType(byte dataType) {
        this.dataType = dataType;
    }

    public int getDataSize() {
        return dataSize;
    }

    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }

    public Number getStreamId() {
        return streamId;
    }

    public void setStreamId(Number streamId) {
        this.streamId = streamId;
    }

    public int getTimer() {
        if (tsCollapsed) {
            return this.timerBase;
        } else {
            return timerBase + timerDelta;
        }
    }

    public void setTimer(int timer) {
        this.timerBase = timer;
        this.timerDelta = 0;
    }

    public void setTimerBase(int timerBase) {
        this.timerBase = timerBase;
    }

    public int getTimerBase() {
        return timerBase;
    }

    public void setTimerDelta(int timerDelta) {
        this.timerDelta = timerDelta;
    }

    public int getTimerDelta() {
        return timerDelta;
    }

    public void setExtended(boolean extended) {
        this.extended = extended;
    }

    public boolean isExtended() {
        return extended;
    }

    public boolean isEmpty() {
        return !((csId + dataType + dataSize + streamId.doubleValue()) > 0d);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + csId;
        result = prime * result + dataType;
        result = prime * result + dataSize;
        result = prime * result + streamId.intValue();
        result = prime * result + getTimer();
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Header)) {
            return false;
        }
        final Header header = (Header) other;
        return (header.getCsId() == csId && header.getDataType() == dataType && header.getDataSize() == dataSize && header.getTimer() == this.getTimer() && header.getStreamId() == streamId);
    }

    @Override
    public Header clone() {
        final Header header = new Header();
        header.setDataType(dataType);
        header.setCsId(csId);
        header.setDataSize(dataSize);
        header.setStreamId(streamId);
        header.setExtended(extended);
        header.setTimerBase(timerBase);
        header.setTimerDelta(timerDelta);
        header.collapseTimeStamps();
        return header;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        dataType = in.readByte();
        csId = in.readInt();
        dataSize = in.readInt();
        streamId = (Number) in.readDouble();
        extended = in.readBoolean();
        timerBase = in.readInt();
        timerDelta = in.readInt();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeByte(dataType);
        out.writeInt(csId);
        out.writeInt(dataSize);
        out.writeDouble(streamId.doubleValue());
        out.writeBoolean(extended);
        out.writeInt(timerBase);
        out.writeInt(timerDelta);
    }

    @Override
    public String toString() {
        // if its new and props are un-set, just return that message
        if (isEmpty()) {
            return "empty";
        } else {
            return "Header [streamId=" + streamId + ", csId=" + csId + ", dataType=" + dataType + ", timerBase=" + timerBase + ", timerDelta=" + timerDelta + ", dataSize=" + dataSize + ", extended=" + extended + "]";
        }
    }

    private transient boolean tsCollapsed = false;

    public void collapseTimeStamps() {
        if (tsCollapsed == false) {
            this.setTimerBase(getTimer());
            this.setTimerDelta(0);
            tsCollapsed = true;
        }
    }
}
