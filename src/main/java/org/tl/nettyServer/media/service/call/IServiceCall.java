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

package org.tl.nettyServer.media.service.call;

/**
 * Container for a Service ServiceCall
 * 回到容器
 */
public interface IServiceCall {

    /**
     * Whether call was successful or not 
     */
    public abstract boolean isSuccess();
 
    public abstract String getServiceMethodName();
 
    public abstract String getServiceName();

    /**
     * Returns array of service method arguments 
     */
    public abstract Object[] getArguments();

    /**
     * Get service call status 
     */
    public abstract byte getStatus();
 
    public long getReadTime();
 
    public long getWriteTime();
 
    public abstract Exception getException();
 
    public abstract void setStatus(byte status);
 
    public abstract void setException(Exception exception);

}