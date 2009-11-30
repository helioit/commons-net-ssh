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

import org.apache.commons.net.ssh.sftp.Response.StatusCode;

public class RemoteFile extends RemoteResource
{
    
    public RemoteFile(SFTPEngine sftp, String path, String handle)
    {
        super(sftp, path, handle);
    }
    
    public RemoteFileInputStream getInputStream()
    {
        return new RemoteFileInputStream(this);
    }
    
    public RemoteFileOutputStream getOutputStream()
    {
        return new RemoteFileOutputStream(this);
    }
    
    public FileAttributes fetchAttributes() throws IOException
    {
        return sftp.make(newRequest(PacketType.FSTAT)) //
                .ensurePacketTypeIs(PacketType.ATTRS) //
                .readFileAttributes();
    }
    
    public long length() throws IOException
    {
        return fetchAttributes().getSize();
    }
    
    public void setLength(long len) throws IOException
    {
        setAttributes(new FileAttributes.Builder().withSize(len).build());
    }
    
    public int read(long fileOffset, byte[] to, int offset, int len) throws IOException
    {
        Response res = sftp.make(newRequest(PacketType.READ).putUINT64(fileOffset).putInt(len));
        switch (res.getType())
        {
        case DATA:
            int recvLen = res.readInt();
            System.arraycopy(res.array(), res.rpos(), to, offset, recvLen);
            return recvLen;
            
        case STATUS:
            res.ensureStatus(StatusCode.EOF);
            return -1;
            
        default:
            throw new SFTPException("Unexpected packet: " + res.getType());
        }
    }
    
    public void write(long fileOffset, byte[] data, int off, int len) throws IOException
    {
        sftp.make( //
                newRequest(PacketType.WRITE) //
                        .putUINT64(fileOffset) //
                        .putInt(len - off) //
                        .putRawBytes(data, off, len) //
                ).ensureStatusOK();
    }
    
    public void setAttributes(FileAttributes attrs) throws IOException
    {
        sftp.make(newRequest(PacketType.FSETSTAT).putFileAttributes(attrs)).ensureStatusOK();
    }
    
    public int getOutgoingPacketOverhead()
    {
        return 1 + // packet type
                4 + // request id
                4 + // handle length
                handle.length() + // handle
                8 + // file offset
                4 + // data length
                4; // packet length
    }
    
}
