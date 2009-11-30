package org.apache.commons.net.ssh;

import java.io.IOException;

import org.apache.commons.net.ssh.xfer.ModeGetter;
import org.apache.commons.net.ssh.xfer.ModeSetter;

public interface FileTransfer
{
    
    void upload(String localPath, String remotePath) throws IOException;
    
    void download(String remotePath, String localPath) throws IOException;
    
    ModeGetter getModeGetter();
    
    void setModeGetter(ModeGetter modeGetter);
    
    ModeSetter getModeSetter();
    
    void setModeSetter(ModeSetter modeSetter);
    
}
