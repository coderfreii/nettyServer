package org.tl.nettyServer.media.media.mpeg;

public class ClockExtensionDescriptorT {
   public byte year; // base 2000, 8-bit
   public byte month; // 1-12, 4-bit
   public byte day; // 1-31, 5-bit
   public byte hour; // 0-23, 5-bit
   public byte minute; // 0-59, 6-bit
   public byte second; // 0-59, 6-bit
   public char microsecond; // 14-bit
}
