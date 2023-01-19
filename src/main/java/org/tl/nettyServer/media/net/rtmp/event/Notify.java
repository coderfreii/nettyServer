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

package org.tl.nettyServer.media.net.rtmp.event;

import org.tl.nettyServer.media.ICommand;
import org.tl.nettyServer.media.buf.BufFacade;
import org.tl.nettyServer.media.buf.ReleaseUtil;
import org.tl.nettyServer.media.service.call.IServiceCall;
import org.tl.nettyServer.media.stream.data.IStreamData;
import org.tl.nettyServer.media.stream.data.IStreamPacket;

import java.io.*;
import java.util.Map;

/**
 * 流通知事件。对于通知，调用/事务ID始终等于零。
 */
public class Notify extends BaseEvent implements ICommand, IStreamData<Notify>, IStreamPacket {

    private static final long serialVersionUID = -6085848257275156569L;

    protected IServiceCall call;

    protected BufFacade data;

    protected byte dataType = TYPE_NOTIFY;
    /**
     * Invoke id / transaction id
     */
    protected int transactionId = 0;

    private int invokeId = 0;

    private Map<String, Object> connectionParams;

    private String action;

    public Notify() {
        super(Type.SERVICE_CALL);
    }

    public Notify(BufFacade data) {
        this(data, null);
    }

    public Notify(BufFacade data, String action) {
        super(Type.STREAM_DATA);
        this.data = data;
        this.action = action;
    }

    public Notify(IServiceCall call) {
        super(Type.SERVICE_CALL);
        this.call = call;
    }

    @Override
    public String toString() {
        return call != null ? String.format("%s: %s", getClass().getSimpleName(), call) : (action != null ? String.format("%s action: %s", getClass().getSimpleName(), action) : getClass().getSimpleName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Notify)) {
            return false;
        }
        Notify other = (Notify) obj;
        if (getConnectionParams() == null && other.getConnectionParams() != null) {
            return false;
        }
        if (getConnectionParams() != null && other.getConnectionParams() == null) {
            return false;
        }
        if (getConnectionParams() != null && !getConnectionParams().equals(other.getConnectionParams())) {
            return false;
        }
        if (getTransactionId() != other.getTransactionId()) {
            return false;
        }
        if (getCall() == null && other.getCall() != null) {
            return false;
        }
        if (getCall() != null && other.getCall() == null) {
            return false;
        }
        if (getCall() != null && !getCall().equals(other.getCall())) {
            return false;
        }
        return true;
    }

    @Override
    protected boolean releaseInternal() {
        if (data != null) {
            if (ReleaseUtil.release(data)) {
                data = null;
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        call = (IServiceCall) in.readObject();
        connectionParams = (Map<String, Object>) in.readObject();
        transactionId = in.readInt();
        byte[] byteBuf = (byte[]) in.readObject();
        if (byteBuf != null) {
            data = BufFacade.buffer(0);
            SerializeUtils.ByteArrayToByteBuffer(byteBuf, data);
        }
        if (log.isTraceEnabled()) {
            log.trace("readExternal - transactionId: {} connectionParams: {} call: {}", transactionId, connectionParams, call);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        if (log.isTraceEnabled()) {
            log.trace("writeExternal - transactionId: {} connectionParams: {} call: {}", transactionId, connectionParams, call);
        }
        out.writeObject(call);
        out.writeObject(connectionParams);
        out.writeInt(transactionId);
        if (data != null) {
            out.writeObject(SerializeUtils.ByteBufferToByteArray(data));
        } else {
            out.writeObject(null);
        }
    }

    /**
     * 将此通知消息复制到将来的注入序列化到内存并反序列化，安全方式。
     */
    public Notify duplicate() throws IOException, ClassNotFoundException {
        return duplicate(true);
    }

    @Override
    public Notify duplicate(boolean serialize) throws IOException, ClassNotFoundException {
        Notify result = new Notify();
        if (serialize) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            writeExternal(oos);
            oos.close();
            byte[] buf = baos.toByteArray();
            baos.close();
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            ObjectInputStream ois = new ObjectInputStream(bais);
            result.readExternal(ois);
            ois.close();
            bais.close();
        } else {
            result.setTimestamp(this.timestamp);
            result.setType(this.getType());

            result.setCall(call);
            result.setConnectionParams(connectionParams);
            result.transactionId = transactionId;
            result.setData(ReleaseUtil.duplicate(this.data));
        }
        //set the action if it exists
        result.setAction(getAction());
        result.setSourceType(sourceType);
        result.setSource(source);
        result.setTimestamp(timestamp);
        return result;
    }

    public int getInvokeId() {
        return invokeId;
    }

    public void setInvokeId(int invokeId) {
        this.invokeId = invokeId;
    }

    public byte getDataType() {
        return dataType;
    }

    public void setData(BufFacade data) {
        this.data = data;
    }

    public void setCall(IServiceCall call) {
        this.call = call;
    }

    public IServiceCall getCall() {
        return this.call;
    }

    public BufFacade getData() {
        return data;
    }

    public int getTransactionId() {
        return transactionId;
    }

    protected void doRelease() {
        call = null;
    }

    public Map<String, Object> getConnectionParams() {
        return connectionParams;
    }

    public void setConnectionParams(Map<String, Object> connectionParams) {
        this.connectionParams = connectionParams;
    }

    public void setAction(String onCueOrOnMeta) {
        this.action = onCueOrOnMeta;
    }

    public String getAction() {
        return action;
    }

    @Override
    public void clear() {
        ReleaseUtil.clear(this.data);
    }
}
