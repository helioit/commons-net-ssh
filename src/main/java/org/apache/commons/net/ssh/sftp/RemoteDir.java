package org.apache.commons.net.ssh.sftp;

public class RemoteDir {
    
    private final SFTP sftp;
    private final String handle;
    
    public RemoteDir(SFTP sftp, String handle) {
        this.sftp = sftp;
        this.handle = handle;
    }
    
}
