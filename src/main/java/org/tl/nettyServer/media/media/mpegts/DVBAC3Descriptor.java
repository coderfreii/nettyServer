package org.tl.nettyServer.media.media.mpegts;
/*
 * ETSI 300 468 descriptor 0x6A(AC-3)
 * Refer to: ETSI EN 300 468 V1.11.1 (2010-04) (SI in DVB systems)
 */
public class DVBAC3Descriptor {
    byte  component_type_flag;
    byte  bsid_flag;
    byte  mainid_flag;
    byte  asvc_flag;
    byte  reserved_flags;
    byte  component_type;
    byte  bsid;
    byte  mainid;
    byte  asvc;
}
