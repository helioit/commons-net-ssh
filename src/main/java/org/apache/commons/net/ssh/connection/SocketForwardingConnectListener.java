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

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.commons.net.ssh.util.StreamCopier;
import org.apache.commons.net.ssh.util.StreamCopier.ErrorCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ConnectListener} that forwards what is received over the channel to a socket and vice-versa.
 */
public class SocketForwardingConnectListener implements ConnectListener
{
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final SocketAddress addr;
    
    /**
     * Create with a {@link SocketAddress} this listener will forward to.
     */
    public SocketForwardingConnectListener(SocketAddress addr)
    {
        this.addr = addr;
    }
    
    /**
     * On connect, confirm the channel and start forwarding.
     */
    public void gotConnect(Channel.Forwarded chan) throws IOException
    {
        log.info("New connection from " + chan.getOriginatorIP() + ":" + chan.getOriginatorPort());
        
        final Socket sock = new Socket();
        sock.setSendBufferSize(chan.getLocalMaxPacketSize());
        sock.setReceiveBufferSize(chan.getRemoteMaxPacketSize());
        
        sock.connect(addr);
        
        // ok so far -- could connect, let's confirm the channel
        chan.confirm();
        
        final ErrorCallback chanCloser = StreamCopier.closeOnErrorCallback(chan);
        
        new StreamCopier("soc2chan", sock.getInputStream(), chan.getOutputStream()) //
                .bufSize(chan.getRemoteMaxPacketSize()) //
                .errorCallback(chanCloser) //
                .daemon(true) //
                .start();
        
        new StreamCopier("chan2soc", chan.getInputStream(), sock.getOutputStream()) //
                .bufSize(chan.getLocalMaxPacketSize()) //
                .errorCallback(chanCloser) //
                .daemon(true) //
                .start();
    }
    
}