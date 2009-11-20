package org.apache.commons.net.ssh.sftp;

import java.io.IOException;

abstract class RemoteResource
{
    
    private final SFTP sftp;
    private final String handle;
    protected final int timeout;
    
    protected RemoteResource(SFTP sftp, String handle)
    {
        this.sftp = sftp;
        this.handle = handle;
        this.timeout = sftp.timeout;
    }
    
    protected Request newRequest(PacketType type)
    {
        Request req = sftp.newRequest(type);
        req.putString(handle);
        return req;
    }
    
    public void close() throws IOException
    {
        Request req = newRequest(PacketType.CLOSE);
        
        send(req);
        
        req.getFuture().get(sftp.timeout).ensureOK();
    }
    
    protected void send(Request req) throws IOException
    {
        sftp.send(req);
    }
    
}
