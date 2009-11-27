package org.apache.commons.net.ssh.util;

import java.io.File;
import java.io.IOException;

public class FileTransferUtil
{
    
    public static File getTargetDirectory(File f, String dirname) throws IOException
    {
        if (f.exists())
            if (f.isDirectory() && !f.getName().equals(dirname))
                f = new File(f, dirname);
            else
                throw new IOException(f + " - already exists as a file; directory required");
        
        if (!f.exists() && !f.mkdir())
            throw new IOException("Failed to create directory: " + f);
        
        return f;
    }
    
    public static File getTargetFile(File f, String filename) throws IOException
    {
        if (f.isDirectory())
            f = new File(f, filename);
        
        if (!f.exists())
        {
            if (!f.createNewFile())
                throw new IOException("Could not create: " + f);
        } else if (f.isDirectory())
            throw new IOException("A directory by the same name already exists: " + f);
        
        return f;
    }
    
}
