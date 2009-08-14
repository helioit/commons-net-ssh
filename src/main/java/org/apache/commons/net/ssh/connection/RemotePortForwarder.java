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
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class RemotePortForwarder extends AbstractForwardedChannelOpener
{
    
    public static final class Forward
    {
        
        private final String address;
        private int port;
        
        public Forward(int port)
        {
            this("", port);
        }
        
        public Forward(String address)
        {
            this(address, 0);
        }
        
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
        
        public String getAddress()
        {
            return address;
        }
        
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
     * Request forwarding from the remote host on specified
     * 
     * @param forward
     * @param listener
     * @return
     * @throws ConnectionException
     * @throws TransportException
     */
    public Forward bind(Forward forward, ConnectListener listener) throws ConnectionException, TransportException
    {
        Buffer reply = conn.sendGlobalRequest(PF_REQ, true, new Buffer() //
                                                                        .putString(forward.address) //
                                                                        .putInt(forward.port)) //
                           .get(conn.getTimeout());
        if (forward.port == 0)
            forward.port = reply.getInt();
        log.info("Remote end listening on {}", forward);
        listeners.put(forward, listener);
        
        /*
         * Connection should forward us "forwarded-tcpip" channels
         */
        if (listeners.isEmpty())
            if (conn.get(getChannelType()) != null && conn.get(getChannelType()) != this)
                throw new AssertionError("Singleton constraint violated");
            else
                conn.attach(this);
        
        return forward;
    }
    
    /**
     * Cancel the forwarding for some {@link Forward}
     * 
     * @param fwd
     * @throws TransportException
     * @throws ConnectionException
     */
    public void cancel(Forward fwd) throws TransportException, ConnectionException
    {
        try {
            conn.sendGlobalRequest(PF_CANCEL, true, new Buffer() //
                                                                .putString(fwd.address) //
                                                                .putInt(fwd.port)) //
                .get(conn.getTimeout());
        } finally {
            listeners.remove(fwd);
            if (listeners.isEmpty())
                conn.forget(this);
        }
    }
    
    public Set<Forward> getActiveForwards()
    {
        return listeners.keySet();
    }
    
    public void handleOpen(Buffer buf) throws ConnectionException, TransportException
    {
        ForwardedTCPIPChannel chan = new ForwardedTCPIPChannel(conn, buf.getInt(), buf.getInt(), buf.getInt(), //
                                                               new Forward(buf.getString(), buf.getInt()), //
                                                               buf.getString(), buf.getInt());
        if (listeners.containsKey(chan.getParentForward()))
            callListener(listeners.get(chan.getParentForward()), chan);
        else
            chan.reject(OpenFailException.Reason.ADMINISTRATIVELY_PROHIBITED, "Forwarding was not requested on ["
                    + chan.getParentForward() + "]");
    }
    
}
