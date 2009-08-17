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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;

/**
 * Handles remote port forwarding.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class RemotePortForwarder extends AbstractForwardedChannelOpener
{
    
    /**
     * Represents a particular forwarding. From RFC 4254, s. 7.1
     * 
     * <pre>
     *    The 'address to bind' and 'port number to bind' specify the IP
     *    address (or domain name) and port on which connections for forwarding
     *    are to be accepted.  Some strings used for 'address to bind' have
     *    special-case semantics.
     * 
     *    o  &quot;&quot; means that connections are to be accepted on all protocol
     *       families supported by the SSH implementation.
     * 
     *    o  &quot;0.0.0.0&quot; means to listen on all IPv4 addresses.
     * 
     *    o  &quot;::&quot; means to listen on all IPv6 addresses.
     * 
     *    o  &quot;localhost&quot; means to listen on all protocol families supported by
     *       the SSH implementation on loopback addresses only ([RFC3330] and
     *       [RFC3513]).
     * 
     *    o  &quot;127.0.0.1&quot; and &quot;::1&quot; indicate listening on the loopback
     *       interfaces for IPv4 and IPv6, respectively.
     * </pre>
     */
    public static final class Forward
    {
        
        private final String address;
        private int port;
        
        /**
         * Creates this forward with address as {@code ""} and specified {@code port}.
         * 
         * @param port
         */
        public Forward(int port)
        {
            this("", port);
        }
        
        /**
         * Creates this forward with specified {@code address} and port as {@code 0}.
         * 
         * @param address
         */
        public Forward(String address)
        {
            this(address, 0);
        }
        
        /**
         * Creates this forward with specified {@code address} and {@code port} number.
         * 
         * @param address
         *            address to bind
         * @param port
         *            port number
         */
        public Forward(String address, int port)
        {
            this.address = address;
            this.port = port;
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (obj == null || getClass() != obj.getClass())
                return false;
            Forward other = (Forward) obj;
            return address.equals(other.address) && port == other.port;
        }
        
        /**
         * Returns the address represented by this forward.
         */
        public String getAddress()
        {
            return address;
        }
        
        /**
         * Returns the port represented by this forward.
         */
        public int getPort()
        {
            return port;
        }
        
        @Override
        public int hashCode()
        {
            return toString().hashCode();
        }
        
        @Override
        public String toString()
        {
            return address + ":" + port;
        }
        
    }
    
    /**
     * A {@code forwarded-tcpip} channel.
     */
    public static class ForwardedTCPIPChannel extends AbstractForwardedChannel
    {
        
        public static final String TYPE = "forwarded-tcpip";
        
        private final Forward fwd;
        
        public ForwardedTCPIPChannel(Connection conn, int recipient, int remoteWinSize, int remoteMaxPacketSize,
                Forward fwd, String origIP, int origPort) throws TransportException
        {
            super(TYPE, conn, recipient, remoteWinSize, remoteMaxPacketSize, origIP, origPort);
            this.fwd = fwd;
        }
        
        /**
         * Returns the forwarding from which this channel originates.
         */
        public Forward getParentForward()
        {
            return fwd;
        }
        
    }
    
    protected static final String PF_REQ = "tcpip-forward";
    protected static final String PF_CANCEL = "cancel-tcpip-forward";
    
    protected final Map<Forward, ConnectListener> listeners = new HashMap<Forward, ConnectListener>();
    
    public RemotePortForwarder(Connection conn)
    {
        super(ForwardedTCPIPChannel.TYPE, conn);
    }
    
    /**
     * Request forwarding from the remote host on the specified {@link Forward}. Forwarded
     * connections will be handled by supplied {@code listener}.
     * <p>
     * If {@code forward} specifies as 0, the returned forward will have the correct port number as
     * informed by remote host.
     * 
     * @param forward
     *            the {@link Forward} to put in place on remote host
     * @param listener
     *            the listener which will handle forwarded connection
     * @return the {@link Forward} which was put into place on the remote host
     * @throws ConnectionException
     *             if there is an error requesting the forwarding
     */
    public synchronized Forward bind(Forward forward, ConnectListener listener) throws ConnectionException,
            TransportException
    {
        Buffer reply = conn.sendGlobalRequest(PF_REQ, true, new Buffer() //
                                                                        .putString(forward.address) //
                                                                        .putInt(forward.port)) //
                           .get(conn.getTimeout());
        if (forward.port == 0)
            forward.port = reply.getInt();
        log.info("Remote end listening on {}", forward);
        listeners.put(forward, listener);
        return forward;
    }
    
    /**
     * Request cancellation of some forwarding.
     * 
     * @param forward
     *            the forward which is being cancelled
     * @throws ConnectionException
     *             if there is an error with the cancellation request
     */
    public synchronized void cancel(Forward forward) throws ConnectionException, TransportException
    {
        try {
            conn.sendGlobalRequest(PF_CANCEL, true, new Buffer() //
                                                                .putString(forward.address) //
                                                                .putInt(forward.port)) //
                .get(conn.getTimeout());
        } finally {
            listeners.remove(forward);
        }
    }
    
    /**
     * Returns the active forwards.
     */
    public Set<Forward> getActiveForwards()
    {
        return listeners.keySet();
    }
    
    /**
     * Internal API. Creates a {@link ForwardedTCPIPChannel} from the {@code CHANNEL_OPEN} request
     * and calls associated {@code ConnectListener} for that forward in a separate thread.
     */
    public void handleOpen(Buffer buf) throws ConnectionException, TransportException
    {
        ForwardedTCPIPChannel chan = new ForwardedTCPIPChannel(conn, buf.getInt(), buf.getInt(), buf.getInt(), //
                                                               new Forward(buf.getString(), buf.getInt()), //
                                                               buf.getString(), buf.getInt());
        if (listeners.containsKey(chan.getParentForward()))
            callListener(listeners.get(chan.getParentForward()), chan);
        else
            chan.reject(OpenFailException.Reason.ADMINISTRATIVELY_PROHIBITED, "Forwarding was not requested on `"
                    + chan.getParentForward() + "`");
    }
    
}
