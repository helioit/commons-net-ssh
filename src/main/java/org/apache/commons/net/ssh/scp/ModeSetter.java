package org.apache.commons.net.ssh.scp;

import java.io.File;
import java.io.IOException;

public interface ModeSetter
{
    
    void setLastAccessedTime(File f, long t) throws IOException;
    
    void setLastModifiedTime(File f, long t) throws IOException;
    
    void setPermissions(File f, String perms) throws IOException;
    
    boolean shouldPreserveTimes();
    
}
