package org.apache.commons.net.ssh.sftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.net.ssh.sftp.Response.StatusCode;
import org.apache.commons.net.ssh.util.FileTransferUtil;
import org.apache.commons.net.ssh.util.IOUtils;
import org.apache.commons.net.ssh.util.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FileTransfer
{
    
    /** Logger */
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final RemoteResourceFilter filter = new RemoteResourceFilter()
    {
        
        public boolean accept(RemoteResourceInfo resource)
        {
            return resource.isDirectory() || resource.isRegularFile();
        }
        
    };
    
    private final SFTP sftp;
    private final RemotePathUtil pathUtil;
    
    public FileTransfer(SFTP sftp)
    {
        this.sftp = sftp;
        this.pathUtil = new RemotePathUtil(sftp);
    }
    
    private void getFile(RemoteResourceInfo remote, File local) throws IOException
    {
        local = FileTransferUtil.getTargetFile(local, remote.getName());
        RemoteFile rf = sftp.open(remote.getPath());
        try
        {
            Pipe.pipe(rf.getInputStream(), new FileOutputStream(local), sftp.getSubsystem().getLocalMaxPacketSize(),
                    true);
        } finally
        {
            IOUtils.closeQuietly(rf);
        }
    }
    
    private void getDir(RemoteResourceInfo remote, File local) throws IOException
    {
        local = FileTransferUtil.getTargetDirectory(local, remote.getName());
        RemoteDir rd = sftp.openDir(remote.getPath());
        List<RemoteResourceInfo> listing;
        try
        {
            listing = rd.scan(filter);
        } finally
        {
            IOUtils.closeQuietly(rd);
        }
        for (RemoteResourceInfo rri : listing)
            get(rri, new File(local.getPath(), rri.getName()));
    }
    
    private void get(RemoteResourceInfo remote, File local) throws IOException
    {
        log.info("Downloading [{}] to [{}]", remote, local);
        if (remote.isDirectory())
            getDir(remote, local);
        else if (remote.isRegularFile())
            getFile(remote, local);
        else
            throw new IOException(remote + " is not a regular file or directory");
    }
    
    public void get(String source, String dest) throws IOException
    {
        PathComponents src = pathUtil.getComponents(source);
        get(new RemoteResourceInfo(src.getParent(), src.getName(), sftp.stat(source)), new File(dest));
    }
    
    private static boolean isNoSuchFileError(SFTPException e)
    {
        return e.getStatusCode() == StatusCode.NO_SUCH_FILE;
    }
    
    private String probeDir(String remote, String dirname) throws IOException
    {
        FileAttributes attrs = null;
        try
        {
            attrs = sftp.stat(remote);
        } catch (SFTPException e)
        {
            if (isNoSuchFileError(e))
            {
                log.debug("probeDir: {} does not exist, creating", remote);
                sftp.makeDir(remote);
                return remote;
            } else
                throw e;
        }
        
        if (attrs.getMode().getType() == FileMode.Type.DIRECTORY)
            if (pathUtil.getComponents(remote).getName().equals(dirname))
            {
                log.debug("probeDir: {} already exists", remote);
                return remote;
            } else
            {
                log.debug("probeDir: {} already exists, path adjusted for {}", remote, dirname);
                return probeDir(RemotePathUtil.adjustForParent(remote, dirname), dirname);
            }
        else
            throw new IOException(attrs.getMode().getType() + " file already exists at " + remote);
    }
    
    private String probeFile(String remote, String filename) throws IOException
    {
        FileAttributes attrs;
        try
        {
            attrs = sftp.stat(remote);
        } catch (SFTPException e)
        {
            if (isNoSuchFileError(e))
            {
                log.debug("probeFile: {} does not exist", remote);
                return remote;
            } else
                throw e;
        }
        if (attrs.getMode().getType() == FileMode.Type.DIRECTORY)
        {
            log.debug("probeFile: {} was directory, path adjusted for {}", remote, filename);
            return RemotePathUtil.adjustForParent(remote, filename);
        } else
        {
            log.debug("probeFile: {} is a {} file that will be replaced", remote, attrs.getMode().getType());
            return remote;
        }
    }
    
    private void putDir(File local, String remote) throws IOException
    {
        String adjusted = probeDir(remote, local.getName());
        for (File f : local.listFiles())
            put(f, adjusted);
    }
    
    private void putFile(File local, String remote) throws IOException
    {
        RemoteFile rf = sftp.open(probeFile(remote, local.getName()), EnumSet.of(OpenMode.WRITE, OpenMode.CREAT,
                OpenMode.TRUNC));
        try
        {
            Pipe.pipe(new FileInputStream(local), rf.getOutputStream(), sftp.getSubsystem().getRemoteMaxPacketSize()
                    - rf.getOutgoingPacketOverhead(), true);
        } finally
        {
            IOUtils.closeQuietly(rf);
        }
    }
    
    private void put(File local, String remote) throws IOException
    {
        log.info("Uploading [{}] to [{}]", local, remote);
        if (local.isDirectory())
            putDir(local, remote);
        else if (local.isFile())
            putFile(local, remote);
        else
            throw new IOException(local + " is not a file or directory");
    }
    
    public void put(String source, String dest) throws IOException
    {
        put(new File(source), dest);
    }
    
}
