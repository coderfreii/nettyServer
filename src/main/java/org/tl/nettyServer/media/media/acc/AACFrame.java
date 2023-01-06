package org.tl.nettyServer.media.media.acc;

import io.netty.buffer.ByteBuf;


/**
 * AAC Frame
 *
 * @author pengliren
 * acc音频数据帧
 */
public class AACFrame {

    private int samplingFrequencyIndex = 0;
    private int sampleRate = 0;
    private int size = 0;
    private int channelConfiguration = 0;
    private int channels = 0;
    private int rdb = 0;
    private int audioObjectType = 2;
    private boolean errorBitsAbsent = true;
    private ByteBuf data;
    private int dataLen;

    public int getSampleRate() {
        return this.sampleRate;
    }

    public void setSampleRate(int rate) {
        this.sampleRate = rate;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
        this.dataLen = size - 7;
    }

    public int getChannels() {
        return this.channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public int getRdb() {
        return this.rdb;
    }

    public void setRdb(int rdb) {
        this.rdb = rdb;
    }

    public int getSampleCount() {
        return (this.rdb + 1) * 1024;
    }

    public int getSamplingFrequencyIndex() {
        return this.samplingFrequencyIndex;
    }

    public void setSamplingFrequencyIndex(int index) {
        this.samplingFrequencyIndex = index;
    }

    public int getChannelConfiguration() {
        return this.channelConfiguration;
    }

    public void setChannelConfiguration(int index) {
        this.channelConfiguration = index;
    }

    public boolean isErrorBitsAbsent() {
        return this.errorBitsAbsent;
    }

    public void setErrorBitsAbsent(boolean absent) {
        this.errorBitsAbsent = absent;
    }

    public int getAudioObjectType() {
        return this.audioObjectType;
    }

    public void setAudioObjectType(int profileType) {
        this.audioObjectType = profileType;
    }

    public ByteBuf getData() {
        return data;
    }

    public void setData(ByteBuf data) {
        this.data = data;
    }

    public void setDataLen(int dataLen) {
        this.dataLen = dataLen;
    }

    public int getDataLen() {

        if (size > 0) dataLen = size - 7;
        return dataLen;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{AACFrame: size: ").append(this.size);
        sb.append(", rate: ").append(this.sampleRate);
        sb.append(", channels: ").append(this.channels);
        sb.append(", audioObjectType: ").append(AACUtils.profileObjectTypeToString(this.audioObjectType));
        sb.append(", samplingFrequencyIndex: ").append(samplingFrequencyIndex).append("}");
        return sb.toString();
    }
}
