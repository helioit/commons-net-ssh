package org.apache.commons.net.ssh.connection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePortForwarder implements ForwardedChannelOpener
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
    
    public static final String PF_REQ = "tcpip-forward";
    public static final String PF_CANCEL = "cancel-tcpip-forward";
    
    public static RemotePortForwarder getInstance(Connection conn)
    {
        RemotePortForwarder rpf = (RemotePortForwarder) conn.get(ForwardedTCPIPChannel.TYPE);
        if (rpf == null)
            conn.attach(rpf = new RemotePortForwarder(conn));
        return rpf;
    }
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final Connection conn;
    
    protected final Map<Forward, ConnectListener> listeners = new HashMap<Forward, ConnectListener>();
    
    private RemotePortForwarder(Connection conn)
    {
        this.conn = conn;
    }
    
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
        if (listeners.isEmpty())
            if (conn.get(getChannelType()) != null && conn.get(getChannelType()) != this)
                throw new AssertionError("Singleton soft-constraint violated");
            else
                conn.attach(this);
        return forward;
    }
    
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
    
    public String getChannelType()
    {
        return ForwardedTCPIPChannel.TYPE;
    }
    
    public void handleOpen(Buffer buf) throws ConnectionException, TransportException
    {
        ForwardedTCPIPChannel chan = new ForwardedTCPIPChannel(conn, buf.getInt(), buf.getInt(), buf.getInt(), //
                                                               new Forward(buf.getString(), buf.getInt()), //
                                                               buf.getString(), buf.getInt());
        if (listeners.containsKey(chan.getParentForward()))
            try {
                listeners.get(chan.getParentForward()).gotConnect(chan);
            } catch (IOException logged) {
                log.warn("Error in ConnectListener callback: {}", logged.toString());
                if (chan.isOpen())
                    chan.sendClose();
                else
                    chan.reject(OpenFailException.Reason.CONNECT_FAILED, "");
            }
        else
            chan.reject(OpenFailException.Reason.ADMINISTRATIVELY_PROHIBITED, "Forwarding was not requested on ["
                    + chan.getParentForward() + "]");
    }
    
}
