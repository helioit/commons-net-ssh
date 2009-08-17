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
package org.apache.commons.net.ssh.connection;

import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Constants.Message;

/**
 * Controls how much data remote end can send before an adjustment notification from us is required.
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class LocalWindow extends Window
{
    int initSize;
    int threshold;
    
    LocalWindow(Channel chan)
    {
        super(chan, true);
    }
    
    public synchronized void check() throws TransportException
    {
        int diff = size - threshold;
        if (diff <= 0)
            growBy(initSize - size);
    }
    
    //    public synchronized void check(int max) throws TransportException
    //    {
    //        int threshold = Math.min(maxPacketSize * 8, max / 4);
    //        int diff = max - size;
    //        if (diff > maxPacketSize && (diff > threshold || size < threshold))
    //            growBy(diff);
    //    }
    
    @Override
    public void init(int initialWinSize, int maxPacketSize)
    {
        initSize = initialWinSize;
        threshold = Math.min(maxPacketSize * 20, initialWinSize / 4);
        super.init(initialWinSize, maxPacketSize);
    }
    
    private synchronized void growBy(int inc) throws TransportException
    {
        sendWindowAdjust(inc);
        expand(inc);
    }
    
    private synchronized void sendWindowAdjust(int inc) throws TransportException
    {
        log.info("Sending SSH_MSG_CHANNEL_WINDOW_ADJUST to #{} for {} bytes", chan.getRecipient(), inc);
        chan.getTransport().writePacket(new Buffer(Message.CHANNEL_WINDOW_ADJUST) //
                                                                                 .putInt(chan.getRecipient()) //
                                                                                 .putInt(inc));
    }
    
}
