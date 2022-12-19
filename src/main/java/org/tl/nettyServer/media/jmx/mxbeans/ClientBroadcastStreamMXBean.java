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

package org.tl.nettyServer.media.jmx.mxbeans;


import org.tl.nettyServer.media.exception.ResourceExistException;
import org.tl.nettyServer.media.exception.ResourceNotFoundException;

import javax.management.MXBean;
import java.io.IOException;

/**
 * Represents live stream broadcasted from client. As Flash Media Server, Red5 supports recording mode for live streams, that is, broadcasted stream has broadcast mode. It can be either "live" or "record" and latter causes server-side application to record broadcasted stream.
 *
 * Note that recorded streams are recorded as FLV files. The same is correct for audio, because NellyMoser codec that Flash Player uses prohibits on-the-fly transcoding to audio formats like MP3 without paying of licensing fee or buying SDK.
 *
 * This type of stream uses two different pipes for live streaming and recording.
 */
@MXBean
public interface ClientBroadcastStreamMXBean {

    public void start();

    public void startPublishing();

    public void stop();

    public void close();

    public void saveAs(String name, boolean isAppend) throws IOException, ResourceNotFoundException, ResourceExistException;

    public String getSaveFilename();

    public String getPublishedName();

    public void setPublishedName(String name);

}
