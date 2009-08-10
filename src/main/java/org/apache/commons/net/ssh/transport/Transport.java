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
import java.net.InetAddress;
import java.net.Socket;

import org.apache.commons.net.ssh.Config;
import org.apache.commons.net.ssh.PacketHandler;
import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.Service;
import org.apache.commons.net.ssh.cipher.Cipher;
import org.apache.commons.net.ssh.compression.Compression;
import org.apache.commons.net.ssh.mac.MAC;
import org.apache.commons.net.ssh.random.Random;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Constants;
import org.apache.commons.net.ssh.util.Constants.DisconnectReason;

/**
 * Transport layer of the SSH protocol.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public interface Transport extends PacketHandler
{
    
    /**
     * Send a disconnection packet with reason as {@link Constants#SSH_DISCONNECT_BY_APPLICATION},
     * and close the session.
     */
    void disconnect();
    
    /**
     * Send a disconnect packet with the given reason, and close this session.
     */
    void disconnect(DisconnectReason reason);
    
    /**
     * Send a disconnect packet with the given reason and message, and close this session.
     * 
     * @param reason
     *            the reason code for this disconnect
     * @param msg
     *            the text message
     */
    void disconnect(DisconnectReason reason, String msg);
    
    String getClientID();
    
    /**
     * Returns the version string used by this client to identify itself to an SSH server, e.g.
     * "NET_3.0"
     * 
     * @return client's version string
     */
    String getClientVersion();
    
    /**
     * Retrieves the {@link Config} associated with this transport.
     */
    Config getConfig();
    
    KeyExchanger getKeyExchanger();
    
    Random getPRNG();
    
    InetAddress getRemoteHost();
    
    int getRemotePort();
    
    String getServerID();
    
    /**
     * Returns the version string as sent by the SSH server for identification purposes.
     * <p>
     * If the session has not yet been initialized via {@link #init}, it will be {@code null}.
     * 
     * @return server's version string
     */
    String getServerVersion();
    
    /**
     * Returns the currently active {@link Service} instance.
     * 
     * @return the currently active service
     */
    Service getService();
    
    int getTimeout();
    
    /**
     * Initialize this session with given {@code socket} by exchanging identification information.
     * 
     * @param socket
     *            the socket on which connection to SSH server has already been established
     * @throws SSHException
     *             if there is an error during session initialization or key exchange
     */
    void init(Socket socket) throws SSHException;
    
    boolean isAuthenticated();
    
    /**
     * Whether this transport is active.
     * <p>
     * The transport is considered to be running if it has been initialized without error and has
     * not been disconnected.
     * 
     * @return {@code true} or {@code false} indicating whether the session is running
     */
    boolean isRunning();
    
    void join() throws TransportException;
    
    /**
     * Request a SSH service represented by a {@link Service} instance.
     * <p>
     * If the request was successful, the active service is set implicitly and a call to
     * {@link #setService(Service)} is not needed.
     * 
     * @param service
     * @throws IOException
     *             if the request failed for any reason
     */
    void reqService(Service service) throws TransportException;
    
    /**
     * Send SSH_MSG_UNIMPLEMENTED in response to the last packet received.
     * 
     * @return the sequence number of packet sent
     * @throws TransportException
     *             if an error occured sending the packet
     */
    long sendUnimplemented() throws TransportException;
    
    /**
     * This method <b>must</b> be called after the session has been authenticated, so that delayed
     * compression may become effective if applicable.
     */
    void setAuthenticated();
    
    void setClientToServerAlgorithms(Cipher cipher, MAC mac, Compression comp);
    
    void setServerToClientAlgorithms(Cipher cipher, MAC mac, Compression comp);
    
    /**
     * Sets the currently active service, to which handling of packets not understood by the
     * transport layer is delegated.
     * <p>
     * Delegation of message-handling is done by calling the {@link Service#handle(Message, Buffer)}
     * method.
     * <p>
     * For this method to be successful, at least one service request must have been successful (not
     * necessarily for the service being set).
     * 
     * @param service
     *            (null-ok)
     */
    void setService(Service service);
    
    void setTimeout(int timeout);
    
    long writePacket(Buffer payload) throws TransportException;
    
}
