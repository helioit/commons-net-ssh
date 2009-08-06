package org.apache.commons.net.ssh.scp;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.connection.ConnectionException;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.IOUtils;

public class SCPUploadClient extends SCPClient
{
    
    protected final ModeGetter modeGetter;
    protected FileFilter fileFilter;
    
    public SCPUploadClient(SSHClient host)
    {
        this(host, null);
    }
    
    public SCPUploadClient(SSHClient host, ModeGetter modeGetter)
    {
        super(host);
        this.modeGetter = modeGetter == null ? new DefaultModeGetter() : modeGetter;
    }
    
    public void setFileFilter(FileFilter fileFilter)
    {
        this.fileFilter = fileFilter;
    }
    
    protected void doCopy(File f) throws IOException
    {
        if (modeGetter.shouldPreserveTimes())
            sendMessage("T" + modeGetter.getLastAccessTime(f) + " 0 " + modeGetter.getLastAccessTime(f) + " 0");
        
        if (f.isDirectory()) {
            log.info("Entering directory `{}`", f.getName());
            sendMessage("D0" + modeGetter.getPermissions(f) + " 0 " + f.getName());
            for (File child : getChildren(f))
                doCopy(child);
            sendMessage("E");
            log.info("Exiting directory `{}`", f.getName());
        } else if (f.isFile()) {
            log.info("Sending `{}`", f.getName());
            InputStream src = new FileInputStream(f);
            sendMessage("C0" + modeGetter.getPermissions(f) + " " + f.length() + " " + f.getName());
            transfer(src, scp.getOutputStream(), scp.getRemoteMaxPacketSize(), f.length());
            sendOK(); // transfer done from our end
            checkResponseOK(); // remote should agree
            IOUtils.closeQuietly(src);
        } else
            throw new IOException(f + " is not a regular file or directory.");
    }
    
    protected File[] getChildren(File f) throws IOException
    {
        File[] files = fileFilter == null ? f.listFiles() : f.listFiles(fileFilter);
        if (files == null)
            throw new IOException("Error listing files in directory: " + f);
        return files;
    }
    
    protected void init(String target) throws ConnectionException, TransportException
    {
        List<String> args = new LinkedList<String>();
        args.add(Arg.SINK.toString());
        args.add(Arg.RECURSIVE.toString());
        args.add(target == null || target.equals("") ? "." : target);
        execSCPWith(args);
    }
    
    protected void sendMessage(String msg) throws IOException
    {
        log.debug("Sending message: {}", msg);
        scp.getOutputStream().write((msg + LF).getBytes());
        scp.getOutputStream().flush();
        checkResponseOK();
    }
    
    @Override
    protected synchronized void startCopy(String sourcePath, String targetPath) throws IOException
    {
        init(targetPath);
        checkResponseOK();
        doCopy(new File(sourcePath));
    }
    
}
