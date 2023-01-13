package org.tl.nettyServer.media.media.cms;

public class STagInfo {
    public STagHead head = new STagHead();
    public byte		flag; //v:video a:audio
    public SAudioInfo	audio = new SAudioInfo();
    public SVideoInfo	video = new SVideoInfo();
}
