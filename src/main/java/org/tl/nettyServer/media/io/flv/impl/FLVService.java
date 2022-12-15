package org.tl.nettyServer.media.io.flv.impl;


import org.tl.nettyServer.media.io.BaseStreamableFileService;
import org.tl.nettyServer.media.io.IStreamableFile;
import org.tl.nettyServer.media.io.flv.IFLVService;

import java.io.File;
import java.io.IOException;

/**
 * A FLVServiceImpl sets up the service and hands out FLV objects to 
 * its callers.
 */
public class FLVService extends BaseStreamableFileService implements IFLVService {

	/**
	 * Generate FLV metadata?
	 */
	private boolean generateMetadata;

	
	@Override
	public String getPrefix() {
		return "flv";
	}

	
	@Override
	public String getExtension() {
		return ".flv";
	}
 
	@Override
	public IStreamableFile getStreamableFile(File file) throws IOException {
		return new FLV(file, generateMetadata);
	}

	/**
	 * Generate metadata or not 
	 */
	public void setGenerateMetadata(boolean generate) {
		generateMetadata = generate;
	}
}
