package org.apache.commons.net.ssh.transport;

import org.apache.commons.net.ssh.ErrorNotifiable;
import org.apache.commons.net.ssh.PacketHandler;

public interface KeyExchanger extends PacketHandler, ErrorNotifiable
{
    
    /**
     * Returns the session identifier computed during key exchange.
     * <p>
     * If the session has not yet been initialized via {@link #open}, it will be {@code null}.
     * 
     * @return session identifier as a byte array
     */
    byte[] getSessionID();
    
    void init(Transport trans);
    
    boolean isKexOngoing();
    
    void startKex(boolean waitForDone) throws TransportException;
    
    void waitForDone() throws TransportException;
    
}