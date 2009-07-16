package org.apache.commons.net.ssh.connection;

import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.transport.Transport;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Constants;

public interface Channel
{
    
    void close() throws TransportException;
    
    int getID();
    
    boolean handle(Constants.Message cmd, Buffer buf) throws ConnectionException, TransportException;
    
    void init(Transport trans, int channelID, int windowSize, int maxPacketSize);
    
    void notifyError(SSHException exception);
    
    void open() throws ConnectionException, TransportException;
    
}