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
package org.apache.commons.net.ssh.transport;

import java.io.IOException;

import org.apache.commons.net.ssh.Config;
import org.apache.commons.net.ssh.ConnInfo;
import org.apache.commons.net.ssh.PacketHandler;
import org.apache.commons.net.ssh.SSHPacket;
import org.apache.commons.net.ssh.Service;
import org.apache.commons.net.ssh.util.Constants.DisconnectReason;

/**
 * Transport layer of the SSH protocol.
 */
public interface Transport extends PacketHandler
{
    
    /**
     * Sets the {@code socket} to be used by this transport; and identification information is exchanged. A
     * {@link TransportException} is thrown in case of SSH protocol version incompatibility.
     * 
     * @param socket
     *            a socket which is already connected to SSH server
     * @throws TransportException
     *             if there is an error during exchange of identification information
     */
    void init(ConnInfo connInfo) throws TransportException;
    
    /**
     * Returns the version string used by this client to identify itself to an SSH server, e.g. "NET_3_0"
     * 
     * @return client's version string
     */
    String getClientVersion();
    
    /**
     * Retrieves the {@link Config} associated with this transport.
     */
    Config getConfig();
    
    /**
     * Returns the timeout that is currently set for blocking operations.
     */
    int getTimeout();
    
    /**
     * Set a timeout for methods that may block, e.g. {@link #reqService(Service)}, {@link KeyExchanger#waitForDone()}.
     * 
     * @param timeout
     *            the timeout in seconds
     */
    void setTimeout(int timeout);
    
    /**
     * Returns the associated {@link KeyExchanger}. This allows {@link KeyExchanger#startKex starting key (re)exchange}
     * and other operations.
     */
    KeyExchanger getKeyExchanger();
    
    /**
     * Returns the hostname to which this transport is connected.
     */
    String getRemoteHost();
    
    /**
     * Returns the port number on the {@link #getRemoteHost() remote host} to which this transport is connected.
     */
    int getRemotePort();
    
    /**
     * Returns the version string as sent by the SSH server for identification purposes, e.g. "OpenSSH_$version".
     * <p>
     * If the transport has not yet been initialized via {@link #init}, it will be {@code null}.
     * 
     * @return server's version string (may be {@code null})
     */
    String getServerVersion();
    
    /**
     * Returns the currently active {@link Service} instance.
     */
    Service getService();
    
    /**
     * Request a SSH service represented by a {@link Service} instance. A separate call to {@link #setService} is not
     * needed.
     * 
     * @param service
     *            the SSH service to be requested
     * @throws IOException
     *             if the request failed for any reason
     */
    void reqService(Service service) throws TransportException;
    
    /**
     * Sets the currently active {@link Service}. Handling of non-transport-layer packets is {@link Service#handle
     * delegated} to that service.
     * <p>
     * For this method to be successful, at least one service request via {@link #reqService} must have been successful
     * (not necessarily for the service being set).
     * 
     * @param service
     *            (null-ok) the {@link Service}
     */
    void setService(Service service);
    
    /**
     * Returns whether the transport thinks it is authenticated.
     */
    boolean isAuthenticated();
    
    /**
     * Informs this transport that authentication has been completed. This method <strong>must</strong> be called after
     * successful authentication, so that delayed compression may become effective if applicable.
     */
    void setAuthenticated();
    
    /**
     * Sends SSH_MSG_UNIMPLEMENTED in response to the last packet received.
     * 
     * @return the sequence number of the packet sent
     * @throws TransportException
     *             if an error occured sending the packet
     */
    long sendUnimplemented() throws TransportException;
    
    /**
     * Write a packet over this transport.
     * <p>
     * The {@code payload} {@link SSHPacket} should have 5 bytes free at the beginning to avoid a performance penalty
     * associated with making space for header bytes (packet length, padding length).
     * 
     * @param payload
     *            the {@link SSHPacket} containing data to send
     * @return sequence number of the sent packet
     * @throws TransportException
     *             if an error occured sending the packet
     */
    long write(SSHPacket payload) throws TransportException;
    
    int getHeartbeatInterval();
    
    void setHeartbeatInterval(int interval);
    
    /**
     * Returns whether this transport is active.
     * <p>
     * The transport is considered to be running if it has been initialized without error via {@link #init} and has not
     * been disconnected.
     */
    boolean isRunning();
    
    /**
     * Joins the thread calling this method to the transport's death. The transport dies of exceptional events.
     * 
     * @throws TransportException
     */
    void join() throws TransportException;
    
    /**
     * Send a disconnection packet with reason as {@link DisconnectReason#BY_APPLICATION}, and closes this transport.
     */
    void disconnect();
    
    /**
     * Send a disconnect packet with the given {@link DisconnectReason reason}, and closes this transport.
     */
    void disconnect(DisconnectReason reason);
    
    /**
     * Send a disconnect packet with the given {@link DisconnectReason reason} and {@code message}, and closes this
     * transport.
     * 
     * @param reason
     *            the reason code for this disconnect
     * @param message
     *            the text message
     */
    void disconnect(DisconnectReason reason, String message);
    
}