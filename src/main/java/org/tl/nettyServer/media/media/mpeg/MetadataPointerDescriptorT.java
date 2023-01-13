package org.tl.nettyServer.media.media.mpeg;

public class MetadataPointerDescriptorT {
    public int metadata_application_format_identifier;
    public int metadata_format_identifier;
    public byte metadata_service_id;
    public byte metadata_locator_record_length;
    public byte MPEG_carriage_flags;
    public char program_number;
    public char transport_stream_location;
    public char transport_stream_id;
}
