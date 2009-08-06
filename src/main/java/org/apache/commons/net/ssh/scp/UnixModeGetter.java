package org.apache.commons.net.ssh.scp;

import java.io.File;
import java.io.IOException;

/*
 * TODO
 * Use Runtime.exec() to run unix commands for getting these values 
 */
public class UnixModeGetter implements ModeGetter
{
    
    public long getLastAccessTime(File f) throws IOException
    {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public long getLastModifiedTime(File f) throws IOException
    {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public String getPermissions(File f) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public boolean shouldPreserveTimes()
    {
        return true;
    }
    
}
