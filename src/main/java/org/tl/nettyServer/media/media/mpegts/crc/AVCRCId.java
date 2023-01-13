package org.tl.nettyServer.media.media.mpegts.crc;

public enum AVCRCId {
    AV_CRC_8_ATM("AV_CRC_8_ATM",1),
    AV_CRC_16_ANSI("AV_CRC_16_ANSI",2),
    AV_CRC_16_CCITT("AV_CRC_16_CCITT",3),
    AV_CRC_32_IEEE("AV_CRC_32_IEEE",4),
    AV_CRC_32_IEEE_LE("AV_CRC_32_IEEE_LE",5),  /*< reversed bitorder version of AV_CRC_32_IEEE */
    AV_CRC_16_ANSI_LE("AV_CRC_16_ANSI_LE",6),  /*< reversed bitorder version of AV_CRC_16_ANSI */
    AV_CRC_24_IEEE("AV_CRC_24_IEEE",7),
    AV_CRC_8_EBU("AV_CRC_8_EBU",8),
    AV_CRC_MAX("AV_CRC_MAX",9);        /*< Not part of public API! Do not use outside libavutil. */

    private String name;
    private int value;
    AVCRCId(String name,int value) {
        this.name = name;
        this.value = value;
    }
    public int getValue(){
        return this.value;
    }

}
