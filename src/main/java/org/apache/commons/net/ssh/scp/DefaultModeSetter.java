package org.apache.commons.net.ssh.scp;

import java.io.File;
import java.io.IOException;

public class DefaultModeSetter implements ModeSetter
{
    
    public void setLastAccessedTime(File f, long t) throws IOException
    {
        // can't do ntn
    }
    
    public void setLastModifiedTime(File f, long t) throws IOException
    {
        // f.setLastModified(t * 1000);
    }
    
    public void setPermissions(File f, String perms) throws IOException
    {
        // TODO: set user's rwx permissions; can't do anything about group and world
    }
    
    public boolean shouldPreserveTimes()
    {
        return false;
    }
    
}
