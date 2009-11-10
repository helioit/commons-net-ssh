package org.apache.commons.net.ssh.sftp;

import java.io.IOException;
import java.io.OutputStream;

public class RemoteFileOutputStream extends OutputStream {
    
    private final RemoteFile rf;
    
    private long offset;
    
    public RemoteFileOutputStream(RemoteFile rf) {
        this.rf = rf;
    }
    
    @Override
    public void write(int b) throws IOException {
        // TODO Auto-generated method stub
        
    }
    
}
