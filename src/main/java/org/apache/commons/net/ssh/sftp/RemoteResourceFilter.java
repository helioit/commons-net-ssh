package org.apache.commons.net.ssh.sftp;

public interface RemoteResourceFilter
{
    
    boolean accept(RemoteResourceInfo resource);
    
}
