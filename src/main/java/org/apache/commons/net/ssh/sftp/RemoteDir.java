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
    
    RemoteDir(SFTPEngine sftp, String path, String handle)
    {
        super(sftp, path, handle);
    }
    
    public List<RemoteResourceInfo> scan(RemoteResourceFilter filter) throws IOException
    {
        List<RemoteResourceInfo> rri = new LinkedList<RemoteResourceInfo>();
        loop: for (;;)
        {
            Response res = sftp.make(newRequest(PacketType.READDIR));
            switch (res.getType())
            {
            
            case NAME:
                final int count = res.readInt();
                for (int i = 0; i < count; i++)
                {
                    final String name = res.readString();
                    res.readString(); // long name - IGNORED - shdve never been in the protocol
                    final FileAttributes attrs = res.readFileAttributes();
                    RemoteResourceInfo inf = new RemoteResourceInfo(path, name, attrs);
                    if (!(name.equals(".") || name.equals("..")) && (filter == null || filter.accept(inf)))
                        rri.add(inf);
                }
                break loop;
            
            case STATUS:
                res.ensureStatus(StatusCode.EOF);
                break loop;
            
            default:
                throw new SFTPException("Unexpected packet: " + res.getType());
            }
        }
        return rri;
    }
    
}
