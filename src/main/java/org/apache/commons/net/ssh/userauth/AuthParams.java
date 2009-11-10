package org.apache.commons.net.ssh.userauth;

import org.apache.commons.net.ssh.transport.Transport;

/**
 * The parameters available to authentication methods
 */
public interface AuthParams
{
    
    /**
     * All userauth requests need to include the name of the next service being requested
     */
    String getNextServiceName();
    
    /**
     * Retrieve the transport which will allow sending packets; retrieving information like the
     * session-id, remote host/port etc. which is needed by some methods.
     */
    Transport getTransport();
    
    /**
     * All userauth requests need to include the username
     */
    String getUsername();
    
}