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

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ssh.ErrorNotifiable;
import org.apache.commons.net.ssh.PacketHandler;
import org.apache.commons.net.ssh.connection.OpenFailException.Reason;
import org.apache.commons.net.ssh.transport.Transport;
import org.apache.commons.net.ssh.transport.TransportException;

/**
 * A channel is the basic medium for application-layer data on top of an SSH transport.
 */
public interface Channel extends Closeable, PacketHandler, ErrorNotifiable
{
    
    /**
     * Direct channels are those that are initiated by us.
     */
    interface Direct extends Channel
    {
        
        void open() throws OpenFailException, ConnectionException, TransportException;
        
    }
    
    /**
     * Forwarded channels are those that are initiated by the server.
     */
    interface Forwarded extends Channel
    {
        
        void confirm() throws TransportException;
        
        String getOriginatorIP();
        
        int getOriginatorPort();
        
        void reject(Reason reason, String message) throws TransportException;
        
    }
    
    /**
     * Close this channel.
     */
    void close() throws TransportException, ConnectionException;
    
    /**
     * Returns whether auto-expansion of local window is set.
     * 
     * @see #setAutoExpand(boolean)
     */
    boolean getAutoExpand();
    
    /**
     * Returns the channel ID
     */
    int getID();
    
    /**
     * Returns the {@code InputStream} for this channel.
     */
    InputStream getInputStream();
    
    /**
     * Returns the maximum packet size that we have specified.
     */
    int getLocalMaxPacketSize();
    
    /**
     * Returns the current local window size.
     */
    int getLocalWinSize();
    
    /**
     * Returns an {@code OutputStream} for this channel.
     */
    OutputStream getOutputStream();
    
    /**
     * Returns the channel ID at the remote end.
     */
    int getRecipient();
    
    /**
     * Returns the maximum packet size as specified by the remote end.
     */
    int getRemoteMaxPacketSize();
    
    /**
     * Returns the current remote window size.
     */
    int getRemoteWinSize();
    
    /**
     * Returns the associated {@link Transport}.
     */
    Transport getTransport();
    
    /**
     * Returns the channel type identifier.
     */
    String getType();
    
    /**
     * Returns whether the channel is open.
     */
    boolean isOpen();
    
    /**
     * Sends an EOF message to the server for this channel; indicating that no more data will be
     * sent by us. The {@code OutputStream} for this channel will be closed and no longer usable.
     */
    void sendEOF() throws TransportException;
    
    /**
     * Set whether local window should automatically expand when data is received, irrespective of
     * whether data has been read from that stream. This is useful e.g. when a remote command
     * produces a lot of output that would fill the local window but you are not interested in
     * reading from its {@code InputStream}.
     * 
     * @param autoExpand
     */
    void setAutoExpand(boolean autoExpand);
    
}
