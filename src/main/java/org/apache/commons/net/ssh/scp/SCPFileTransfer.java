package org.apache.commons.net.ssh.scp;

import java.io.IOException;

import org.apache.commons.net.ssh.SessionFactory;
import org.apache.commons.net.ssh.xfer.AbstractFileTransfer;
import org.apache.commons.net.ssh.xfer.FileTransfer;

public class SCPFileTransfer extends AbstractFileTransfer implements FileTransfer
{
    private final SessionFactory sessionFactory;
    
    public SCPFileTransfer(SessionFactory sessionFactory)
    {
        this.sessionFactory = sessionFactory;
    }
    
    public SCPDownloadClient newSCPDownloadClient()
    {
        return new SCPDownloadClient(sessionFactory, getModeSetter());
    }
    
    public SCPUploadClient newSCPUploadClient()
    {
        return new SCPUploadClient(sessionFactory, getModeGetter());
    }
    
    public void download(String remotePath, String localPath) throws IOException
    {
        newSCPDownloadClient().copy(remotePath, localPath);
    }
    
    public void upload(String localPath, String remotePath) throws IOException
    {
        newSCPUploadClient().copy(localPath, remotePath);
    }
    
}
