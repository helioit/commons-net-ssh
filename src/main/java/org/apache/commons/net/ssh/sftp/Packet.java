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

import org.apache.commons.net.ssh.util.Buffer;

public class Packet extends Buffer
{
    
    public Packet()
    {
        super();
    }
    
    public Packet(Packet pk)
    {
        super(pk);
    }
    
    public FileAttributes readFileAttributes()
    {
        return new FileAttributes(this);
    }
    
    public Packet putFileAttributes(FileAttributes fa)
    {
        return (Packet) putBuffer(fa.toBuffer());
    }
    
    public PacketType readType()
    {
        return PacketType.fromByte(readByte());
    }
    
    public Packet putType(PacketType type)
    {
        return (Packet) putByte(type.toByte());
    }
    
}
