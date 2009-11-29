package org.apache.commons.net.ssh.sftp;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumSet;

import org.apache.commons.net.ssh.sftp.Response.StatusCode;
import org.apache.commons.net.ssh.util.FileTransferUtil;
import org.apache.commons.net.ssh.util.IOUtils;
import org.apache.commons.net.ssh.util.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FileTransferHandler
{
    
    /** Logger */
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final SFTPEngine sftp;
    private final PathUtil pathUtil;
    
    private static final FileFilter defaultLocalFilter = new FileFilter()
    {
        public boolean accept(File pathname)
        {
            return true;
        }
    };
    
    private static final RemoteResourceFilter defaultRemoteFilter = new RemoteResourceFilter()
    {
        public boolean accept(RemoteResourceInfo resource)
        {
            return true;
        }
    };
    
    private volatile FileFilter localFilter;
    private volatile RemoteResourceFilter remoteFilter;
    
    public FileTransferHandler(SFTPEngine sftp)
    {
        this.sftp = sftp;
        this.pathUtil = new PathUtil(sftp);
        localFilter = defaultLocalFilter;
        remoteFilter = defaultRemoteFilter;
    }
    
    public void setUploadFilter(FileFilter localFilter)
    {
        this.localFilter = (localFilter == null) ? defaultLocalFilter : localFilter;
    }
    
    public void setDownloadFilter(RemoteResourceFilter remoteFilter)
    {
        this.remoteFilter = (remoteFilter == null) ? defaultRemoteFilter : remoteFilter;
    }
    
    private void downloadFile(RemoteResourceInfo remote, File local) throws IOException
    {
        local = FileTransferUtil.getTargetFile(local, remote.getName());
        RemoteFile rf = sftp.open(remote.getPath());
        Pipe.pipe(rf.getInputStream(), new FileOutputStream(local), sftp.getSubsystem().getLocalMaxPacketSize());
        rf.close();
    }
    
    private void downloadDir(RemoteResourceInfo remote, File local) throws IOException
    {
        local = FileTransferUtil.getTargetDirectory(local, remote.getName());
        RemoteDir rd = sftp.openDir(remote.getPath());
        for (RemoteResourceInfo rri : rd.scan(remoteFilter))
            download(rri, new File(local.getPath(), rri.getName()));
        rd.close();
    }
    
    private void download(RemoteResourceInfo remote, File local) throws IOException
    {
        log.info("Downloading [{}] to [{}]", remote, local);
        if (remote.isDirectory())
            downloadDir(remote, local);
        else if (remote.isRegularFile())
            downloadFile(remote, local);
        else
            throw new IOException(remote + " is not a regular file or directory");
    }
    
    public void download(String source, String dest) throws IOException
    {
        PathComponents src = pathUtil.getComponents(source);
        download(new RemoteResourceInfo(src.getParent(), src.getName(), sftp.stat(source)), new File(dest));
    }
    
    private String probeDir(String remote, String dirname) throws IOException
    {
        FileAttributes attrs = null;
        try
        {
            attrs = sftp.stat(remote);
        } catch (SFTPException e)
        {
            if (e.getStatusCode() == StatusCode.NO_SUCH_FILE)
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
                return probeDir(PathUtil.adjustForParent(remote, dirname), dirname);
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
            if (e.getStatusCode() == StatusCode.NO_SUCH_FILE)
            {
                log.debug("probeFile: {} does not exist", remote);
                return remote;
            } else
                throw e;
        }
        if (attrs.getMode().getType() == FileMode.Type.DIRECTORY)
        {
            log.debug("probeFile: {} was directory, path adjusted for {}", remote, filename);
            return PathUtil.adjustForParent(remote, filename);
        } else
        {
            log.debug("probeFile: {} is a {} file that will be replaced", remote, attrs.getMode().getType());
            return remote;
        }
    }
    
    private void uploadDir(File local, String remote) throws IOException
    {
        String adjusted = probeDir(remote, local.getName());
        for (File f : local.listFiles(localFilter))
            upload(f, adjusted);
    }
    
    private void uploadFile(File local, String remote) throws IOException
    {
        RemoteFile rf = sftp.open(probeFile(remote, local.getName()), EnumSet.of(OpenMode.WRITE, OpenMode.CREAT,
                OpenMode.TRUNC));
        try
        {
            Pipe.pipe(new FileInputStream(local), rf.getOutputStream(), sftp.getSubsystem().getRemoteMaxPacketSize()
                    - rf.getOutgoingPacketOverhead());
        } finally
        {
            IOUtils.closeQuietly(rf);
        }
    }
    
    private void upload(File local, String remote) throws IOException
    {
        log.info("Uploading [{}] to [{}]", local, remote);
        if (local.isDirectory())
            uploadDir(local, remote);
        else if (local.isFile())
            uploadFile(local, remote);
        else
            throw new IOException(local + " is not a file or directory");
    }
    
    public void upload(String source, String dest) throws IOException
    {
        upload(new File(source), dest);
    }
    
}
