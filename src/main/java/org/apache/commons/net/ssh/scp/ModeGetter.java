package org.apache.commons.net.ssh.scp;

import java.io.File;
import java.io.IOException;

public interface ModeGetter
{
    
    long getLastAccessTime(File f) throws IOException;
    
    long getLastModifiedTime(File f) throws IOException;
    
    String getPermissions(File f) throws IOException;
    
    boolean shouldPreserveTimes();
    
}
