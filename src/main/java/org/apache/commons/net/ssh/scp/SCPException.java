/**
 * 
 */
package org.apache.commons.net.ssh.scp;

import org.apache.commons.net.ssh.SSHException;

public class SCPException extends SSHException
{
    public SCPException(String message)
    {
        super(message);
    }
    
    public SCPException(String message, Throwable cause)
    {
        super(message, cause);
    }
}