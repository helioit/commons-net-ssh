/**
 * 
 */
package org.apache.commons.net.ssh.connection;

import java.io.IOException;


public interface ConnectListener
{
    
    void gotConnect(Channel.Forwarded chan) throws IOException;
    
}