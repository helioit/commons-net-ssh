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

import org.apache.commons.net.ssh.transport.Transport;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Future;

/**
 * Connection layer of the SSH protocol.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public interface Connection
{
    
    /**
     * Send an SSH global request.
     * 
     * @param name
     *            request name
     * @param wantReply
     *            whether a reply is requested
     * @param specifics
     *            {@link Buffer} containing fields specific to the request
     * @return a {@link Future} for the reply data (in case {@code wantReply} is true) which allows
     *         waiting on the reply, or {@code null} if a reply is not requested.
     * @throws TransportException
     *             if there is an error sending the request
     */
    public Future<Buffer, ConnectionException> sendGlobalRequest(String name, boolean wantReply, Buffer specifics)
            throws TransportException;
    
    /**
     * Attach a {@link Channel} to this connection. A channel must be attached to the connection if
     * it is to receive any channel-specific data that is received.
     */
    void attach(Channel chan);
    
    /**
     * Attach a {@link ForwardedChannelOpener} to this connection, which will be delegated opening
     * of any {@code CHANNEL_OPEN} packets {@link ForwardedChannelOpener#getChannelType() for which
     * it is responsible}.
     */
    void attach(ForwardedChannelOpener opener);
    
    /**
     * Forget an attached {@link Channel}.
     */
    void forget(Channel chan);
    
    /**
     * Forget an attached {@link ForwardedChannelOpener}.
     */
    void forget(ForwardedChannelOpener handler);
    
    /**
     * Returns an attached {@link Channel} of specified channel-id, or {@code null} if no such
     * channel was attached
     */
    Channel get(int id);
    
    /**
     * Returns an attached {@link ForwardedChannelOpener} of specified channel-type, or {@code null}
     * if no such channel was attached
     */
    ForwardedChannelOpener get(String chanType);
    
    /**
     * Get the maximum packet size for the local window this connection recommends to any
     * {@link Channel}'s that ask for it.
     */
    int getMaxPacketSize();
    
    /**
     * Get the {@code timeout} this connection uses for blocking operations and recommends to any
     * {@link Channel other} {@link ForwardedChannelOpener classes} that ask for it.
     */
    int getTimeout();
    
    /**
     * Get the associated {@link Transport}.
     */
    Transport getTransport();
    
    /**
     * Get the size for the local window this connection recommends to any {@link Channel}'s that
     * ask for it.
     */
    int getWindowSize();
    
    /**
     * Wait for the situation that no channels are attached (e.g., got closed).
     */
    void join() throws InterruptedException;
    
    /**
     * Returns an available ID a {@link Channel} can rightfully claim.
     */
    int nextID();
    
    /**
     * Send a {@code SSH_MSG_OPEN_FAILURE} for specified {@code Reason} and {@code message}.
     * 
     * @param recipient
     * @param reason
     * @param message
     * @throws TransportException
     */
    void sendOpenFailure(int recipient, OpenFailException.Reason reason, String message) throws TransportException;
    
    /**
     * Set the maximum packet size for the local window this connection recommends to any
     * {@link Channel}'s that ask for it.
     */
    void setMaxPacketSize(int maxPacketSize);
    
    /**
     * Set the {@code timeout} this connection uses for blocking operations and recommends to any
     * {@link Channel other} {@link ForwardedChannelOpener classes} that ask for it.
     */
    void setTimeout(int timeout);
    
    /**
     * Set the size for the local window this connection recommends to any {@link Channel}'s that
     * ask for it.
     */
    void setWindowSize(int windowSize);
}
