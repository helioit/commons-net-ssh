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
package org.apache.commons.net.ssh;

import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Constants;
import org.apache.commons.net.ssh.util.Constants.Message;

public class SSHPacket extends Buffer<SSHPacket>
{
    
    public SSHPacket()
    {
        super();
    }
    
    public SSHPacket(int size)
    {
        super(size);
    }
    
    public SSHPacket(byte[] data)
    {
        super(data);
    }
    
    /**
     * Constructs new buffer for the specified SSH packet and reserves the needed space (5 bytes) for the packet header.
     * 
     * @param cmd
     *            the SSH command
     */
    public SSHPacket(Constants.Message msg)
    {
        super();
        rpos = wpos = 5;
        putMessageID(msg);
    }
    
    /**
     * Reads an SSH byte and returns it as {@link Constants.Message}
     * 
     * @return the message identifier
     */
    public Message readMessageID()
    {
        byte b = readByte();
        Message cmd = Message.fromByte(b);
        if (cmd == null)
            throw new BufferException("Unknown message ID: " + b);
        return cmd;
    }
    
    /**
     * Writes a byte indicating the SSH message identifier
     * 
     * @param msg
     *            the identifier as a {@link Constants.Message} type
     * @return this
     */
    public SSHPacket putMessageID(Message msg)
    {
        return putByte(msg.toByte());
    }
    
}