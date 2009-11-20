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
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.net.ssh.sftp.Response.StatusCode;

public class RemoteDir extends RemoteResource
{
    
    RemoteDir(SFTP sftp, String handle)
    {
        super(sftp, handle);
    }
    
    public List<RemoteResourceInfo> scan() throws IOException
    {
        List<RemoteResourceInfo> rfo = new LinkedList<RemoteResourceInfo>();
        loop: for (;;)
        {
            Request req = newRequest(PacketType.READDIR);
            send(req);
            Response res = req.getFuture().get(timeout);
            switch (res.getType())
            {
            case NAME:
                final int count = res.readInt();
                for (int i = 0; i < count; i++)
                    rfo.add(new RemoteResourceInfo(res.readString(), res.readString(), res.readFileAttributes()));
                break loop;
            case STATUS:
                res.ensureStatus(StatusCode.EOF);
                break loop;
            default:
                throw new SFTPException("Unexpected packet: " + res.getType());
            }
        }
        return rfo;
    }
    
}
