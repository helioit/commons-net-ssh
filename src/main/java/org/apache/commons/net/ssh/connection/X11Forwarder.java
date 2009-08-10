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

import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class X11Forwarder implements ForwardedChannelOpener
{
    
    public static class X11Channel extends AbstractForwardedChannel
    {
        
        public static final String TYPE = "x11";
        
        protected X11Channel(Connection conn, int recipient, int remoteWinSize, int remoteMaxPacketSize, String origIP,
                int origPort)
        {
            super(TYPE, conn, recipient, remoteWinSize, remoteMaxPacketSize, origIP, origPort);
        }
        
    }
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final Connection conn;
    protected final ConnectListener listener;
    
    public X11Forwarder(Connection conn, ConnectListener listener)
    {
        this.conn = conn;
        this.listener = listener;
        conn.attach(this);
    }
    
    public String getChannelType()
    {
        return X11Channel.TYPE;
    }
    
    public void handleOpen(Buffer buf) throws ConnectionException, TransportException
    {
        X11Channel chan = new X11Channel(conn, buf.getInt(), buf.getInt(), buf.getInt(), buf.getString(), buf.getInt());
        try {
            listener.gotConnect(chan);
        } catch (IOException ioe) {
            if (chan.isOpen())
                chan.close();
        }
    }
    
}
