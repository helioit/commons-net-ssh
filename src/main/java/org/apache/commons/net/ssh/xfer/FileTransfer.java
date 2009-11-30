package org.apache.commons.net.ssh.xfer;

import java.io.IOException;


public interface FileTransfer
{
    
    void upload(String localPath, String remotePath) throws IOException;
    
    void download(String remotePath, String localPath) throws IOException;
    
    ModeGetter getModeGetter();
    
    void setModeGetter(ModeGetter modeGetter);
    
    ModeSetter getModeSetter();
    
    void setModeSetter(ModeSetter modeSetter);
    
}
