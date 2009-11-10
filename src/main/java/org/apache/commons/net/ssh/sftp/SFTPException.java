package org.apache.commons.net.ssh.sftp;

import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.sftp.Response.StatusCode;
import org.apache.commons.net.ssh.util.FriendlyChainer;
import org.apache.commons.net.ssh.util.Constants.DisconnectReason;

public class SFTPException extends SSHException
{
    
    public static final FriendlyChainer<SFTPException> chainer = new FriendlyChainer<SFTPException>()
    {
        
        public SFTPException chain(Throwable t)
        {
            if (t instanceof SSHException)
                return (SFTPException) t;
            else
                return new SFTPException(t);
        }
        
    };
    
    public SFTPException()
    {
        super();
    }
    
    public SFTPException(DisconnectReason code)
    {
        super(code);
    }
    
    public SFTPException(DisconnectReason code, String message)
    {
        super(code, message);
    }
    
    public SFTPException(DisconnectReason code, String message, Throwable cause)
    {
        super(code, message, cause);
    }
    
    public SFTPException(DisconnectReason code, Throwable cause)
    {
        super(code, cause);
    }
    
    public SFTPException(String message)
    {
        super(message);
    }
    
    public SFTPException(String message, Throwable cause)
    {
        super(message, cause);
    }
    
    public SFTPException(Throwable cause)
    {
        super(cause);
    }
    
    private StatusCode sc;
    
    public StatusCode getStatusCode()
    {
        return (sc == null) ? StatusCode.UNKNOWN : sc;
        
    }
    
    public SFTPException(StatusCode sc, String msg)
    {
        this(msg);
        this.sc = sc;
    }
    
}
