package org.apache.commons.net.ssh;

import org.apache.commons.net.ssh.connection.ConnectionException;
import org.apache.commons.net.ssh.connection.Session;
import org.apache.commons.net.ssh.transport.TransportException;

public interface SessionFactory
{
    
    Session startSession() throws ConnectionException, TransportException;
    
}
