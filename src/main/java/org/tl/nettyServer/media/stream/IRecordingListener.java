package org.tl.nettyServer.media.stream;


import org.tl.nettyServer.media.net.rtmp.conn.IConnection;
import org.tl.nettyServer.media.scope.IScope;
import org.tl.nettyServer.media.stream.consumer.FileConsumer;

/**
 * Recording listener interface.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface IRecordingListener extends IStreamListener {
 
    public boolean init(IConnection conn, String name, boolean isAppend);
 
    public boolean init(IScope scope, String name, boolean isAppend);
 
    public void start();
 
    public void stop(); 
    
    public void packetReceived(IBroadcastStream stream, IStreamPacket packet);
 
    public boolean isRecording();
 
    public boolean isAppending();
 
    public FileConsumer getFileConsumer();
 
    public void setFileConsumer(FileConsumer recordingConsumer);
 
    public String getFileName();
 
    public void setFileName(String fileName); 
}
