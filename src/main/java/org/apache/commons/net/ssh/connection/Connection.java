package org.apache.commons.net.ssh.connection;

import org.apache.commons.net.ssh.transport.Transport;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Future;

public interface Connection
{
    
    public Future<Buffer, ConnectionException> sendGlobalRequest(String name, boolean wantReply, Buffer specifics)
            throws TransportException;
    
    void attach(Channel chan);
    
    void attach(ForwardedChannelOpener handler);
    
    void forget(Channel chan);
    
    void forget(ForwardedChannelOpener handler);
    
    Channel get(int id);
    
    ForwardedChannelOpener get(String chanType);
    
    int getMaxPacketSize();
    
    int getTimeout();
    
    Transport getTransport();
    
    int getWindowSize();
    
    void join() throws InterruptedException;
    
    int nextID();
    
    void setMaxPacketSize(int maxPacketSize);
    
    void setTimeout(int timeout);
    
    void setWindowSize(int windowSize);
    
}
