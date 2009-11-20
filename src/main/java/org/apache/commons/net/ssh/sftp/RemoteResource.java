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

import java.io.IOException;

abstract class RemoteResource
{
    
    private final SFTP sftp;
    private final String handle;
    protected final int timeout;
    
    protected RemoteResource(SFTP sftp, String handle)
    {
        this.sftp = sftp;
        this.handle = handle;
        this.timeout = sftp.timeout;
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
        send(req);
        req.getFuture().get(sftp.timeout).ensureStatusOK();
    }
    
    protected void send(Request req) throws IOException
    {
        sftp.send(req);
    }
    
}
