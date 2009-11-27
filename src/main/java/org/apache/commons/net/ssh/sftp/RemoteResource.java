/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.net.ssh.sftp;

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class RemoteResource implements Closeable
{
    
    protected final SFTP sftp;
    protected final String path;
    protected final String handle;
    
    /** Logger */
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected RemoteResource(SFTP sftp, String path, String handle)
    {
        this.sftp = sftp;
        this.path = path;
        this.handle = handle;
    }
    
    public String getPath()
    {
        return path;
    }
    
    protected Request newRequest(PacketType type)
    {
        Request req = sftp.newRequest(type);
        req.putString(handle);
        return req;
    }
    
    public void close() throws IOException
    {
        Request req = newRequest(PacketType.CLOSE);
        log.info("Closing {}", this);
        sftp.send(req);
        req.getFuture().get(sftp.timeout).ensureStatusOK();
    }
    
    @Override
    public String toString()
    {
        return "RemoteResource{" + path + "}";
    }
    
}
