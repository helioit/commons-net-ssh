package org.apache.commons.net.ssh.connection;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ssh.ErrorNotifiable;
import org.apache.commons.net.ssh.PacketHandler;
import org.apache.commons.net.ssh.connection.OpenFailException.Reason;
import org.apache.commons.net.ssh.transport.Transport;
import org.apache.commons.net.ssh.transport.TransportException;

public interface Channel extends Closeable, PacketHandler, ErrorNotifiable
{
    
    interface Direct extends Channel
    {
        
        void open() throws ConnectionException, TransportException;
        
    }
    
    interface Forwarded extends Channel
    {
        
        void confirm() throws TransportException;
        
        String getOriginatorIP();
        
        int getOriginatorPort();
        
        void reject(Reason reason, String message) throws TransportException;
        
    }
    
    void close() throws TransportException, ConnectionException;
    
    int getID();
    
    InputStream getInputStream();
    
    int getLocalMaxPacketSize();
    
    int getLocalWinSize();
    
    OutputStream getOutputStream();
    
    int getRecipient();
    
    int getRemoteMaxPacketSize();
    
    int getRemoteWinSize();
    
    Transport getTransport();
    
    String getType();
    
    boolean isOpen();
    
    void sendEOF() throws TransportException;
    
}
