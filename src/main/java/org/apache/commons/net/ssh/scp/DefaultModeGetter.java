package org.apache.commons.net.ssh.scp;

import java.io.File;
import java.io.IOException;

public class DefaultModeGetter implements ModeGetter
{
    
    public long getLastAccessTime(File f)
    {
        //return f.lastModified() / 1000;
        return 0;
    }
    
    public long getLastModifiedTime(File f)
    {
        //return f.lastModified() / 1000;
        return 0;
    }
    
    public String getPermissions(File f) throws IOException
    {
        if (f.isDirectory())
            return "755";
        else if (f.isFile())
            return "644";
        else
            throw new IOException("Unsupported file type: " + f);
    }
    
    public boolean shouldPreserveTimes()
    {
        return false;
    }
    
}
