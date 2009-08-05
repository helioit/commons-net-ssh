package org.apache.commons.net.ssh.scp;

import java.io.IOException;

import org.apache.commons.net.ssh.SSHClient;

public class SCPDownloadClient extends SCPClient
{
    
    protected final ModeSetter modeSetter;
    
    public SCPDownloadClient(SSHClient host)
    {
        this(host, null);
    }
    
    public SCPDownloadClient(SSHClient host, ModeSetter modeSetter)
    {
        super(host);
        this.modeSetter = modeSetter == null ? new DefaultModeSetter() : modeSetter;
    }
    
    @Override
    public int copy(String source, String target) throws IOException
    {
        // TODO
        return 0;
    }
    
}
