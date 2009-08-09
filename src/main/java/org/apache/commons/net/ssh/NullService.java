package org.apache.commons.net.ssh;

import org.apache.commons.net.ssh.transport.Transport;

public class NullService extends AbstractService
{
    
    public NullService(Transport trans)
    {
        super("null-service", trans);
    }
    
    public void notifyError(SSHException error)
    {
    }
    
}
