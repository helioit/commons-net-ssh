package org.apache.commons.net.ssh.scp;

import java.io.File;
import java.io.IOException;

/*
 * TODO
 * Use Runtime.exec() to run unix commands for setting these values 
 */
public class UnixModeSetter implements ModeSetter
{
    
    public void setLastAccessedTime(File f, long t) throws IOException
    {
        // TODO Auto-generated method stub
        
    }
    
    public void setLastModifiedTime(File f, long t) throws IOException
    {
        // TODO Auto-generated method stub
        
    }
    
    public void setPermissions(File f, String perms) throws IOException
    {
        // TODO Auto-generated method stub
        
    }
    
    public boolean shouldPreserveTimes()
    {
        return true;
    }
    
}
