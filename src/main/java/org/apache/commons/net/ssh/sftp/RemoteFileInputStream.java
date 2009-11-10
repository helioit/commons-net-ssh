package org.apache.commons.net.ssh.sftp;

import java.io.IOException;
import java.io.InputStream;

public class RemoteFileInputStream extends InputStream {
    
    private final RemoteFile rf;
    
    private long offset;
    
    public RemoteFileInputStream(RemoteFile rf) {
        this.rf = rf;
    }
    
    @Override
    public int read() throws IOException {
        return 0;
    }
    
}
