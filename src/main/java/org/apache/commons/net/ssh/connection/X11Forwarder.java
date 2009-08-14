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

/**
 * Handles forwarded {@code x11} channels. The actual request to forward X11 should be made from the
 * specific {@link Session}.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class X11Forwarder extends AbstractForwardedChannelOpener
{
    
    /**
     * An {@code x11} forwarded channel.
     */
    public static class X11Channel extends AbstractForwardedChannel
    {
        
        public static final String TYPE = "x11";
        
        public X11Channel(Connection conn, int recipient, int remoteWinSize, int remoteMaxPacketSize, String origIP,
                int origPort)
        {
            super(TYPE, conn, recipient, remoteWinSize, remoteMaxPacketSize, origIP, origPort);
        }
        
    }
    
    protected final ConnectListener listener;
    
    /**
     * Creates and registers itself with {@code conn}.
     * 
     * @param conn
     *            connection layer
     * @param listener
     *            listener which will be delegated {@link X11Channel}'s to handle
     */
    public X11Forwarder(Connection conn, ConnectListener listener)
    {
        super(X11Channel.TYPE, conn);
        this.listener = listener;
        conn.attach(this);
    }
    
    /**
     * Internal API
     */
    public void handleOpen(Buffer buf) throws ConnectionException, TransportException
    {
        callListener(listener, new X11Channel(conn, buf.getInt(), buf.getInt(), buf.getInt(), buf.getString(),
                                              buf.getInt()));
    }
    
    /**
     * Stop handling {@code x11} channel open requests. De-registers itself with connection layer.
     */
    public void stop()
    {
        conn.forget(this);
    }
    
}
