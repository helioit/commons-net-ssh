package org.apache.commons.net.ssh.sftp;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumSet;

import org.apache.commons.net.ssh.FileTransfer;
import org.apache.commons.net.ssh.sftp.Response.StatusCode;
import org.apache.commons.net.ssh.util.IOUtils;
import org.apache.commons.net.ssh.util.Pipe;
import org.apache.commons.net.ssh.xfer.AbstractFileTransfer;
import org.apache.commons.net.ssh.xfer.FileTransferUtil;
import org.apache.commons.net.ssh.xfer.ModeGetter;
import org.apache.commons.net.ssh.xfer.ModeSetter;

public class SFTPFileTransfer extends AbstractFileTransfer implements FileTransfer
{
    
    private final SFTPEngine sftp;
    private final PathUtil pathUtil;
    
    private volatile FileFilter uploadFilter = defaultLocalFilter;
    private volatile RemoteResourceFilter downloadFilter = defaultRemoteFilter;
    
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
    
    public SFTPFileTransfer(SFTPEngine sftp)
    {
        this.sftp = sftp;
        this.pathUtil = new PathUtil(sftp);
    }
    
    public void upload(String source, String dest) throws IOException
    {
        new Uploader(getModeGetter(), getUploadFilter()).upload(new File(source), dest);
    }
    
    public void download(String source, String dest) throws IOException
    {
        PathComponents src = pathUtil.getComponents(source);
        new Downloader(getModeSetter(), getDownloadFilter()).download(new RemoteResourceInfo(src.getParent(), src
                .getName(), sftp.stat(source)), new File(dest));
    }
    
    public void setUploadFilter(FileFilter uploadFilter)
    {
        this.uploadFilter = (this.uploadFilter == null) ? defaultLocalFilter : uploadFilter;
    }
    
    public void setDownloadFilter(RemoteResourceFilter downloadFilter)
    {
        this.downloadFilter = (this.downloadFilter == null) ? defaultRemoteFilter : downloadFilter;
    }
    
    public FileFilter getUploadFilter()
    {
        return uploadFilter;
    }
    
    public RemoteResourceFilter getDownloadFilter()
    {
        return downloadFilter;
    }

    private class Downloader
    {
        
        private final ModeSetter modeSetter;
        private final RemoteResourceFilter filter;
        
        Downloader(ModeSetter modeSetter, RemoteResourceFilter filter)
        {
            this.modeSetter = modeSetter;
            this.filter = filter;
        }
        
        private void setAttributes(RemoteResourceInfo remote, File local) throws IOException
        {
            final FileAttributes attrs = remote.getAttributes();
            modeSetter.setPermissions(local, attrs.getMode().getPermissionsMask());
            if (modeSetter.preservesTimes() && attrs.has(FileAttributes.Flag.ACMODTIME))
            {
                modeSetter.setLastAccessedTime(local, attrs.getAtime());
                modeSetter.setLastModifiedTime(local, attrs.getMtime());
            }
        }
        
        private void downloadFile(RemoteResourceInfo remote, File local) throws IOException
        {
            local = FileTransferUtil.getTargetFile(local, remote.getName());
            setAttributes(remote, local);
            RemoteFile rf = sftp.open(remote.getPath());
            Pipe.pipe(rf.getInputStream(), new FileOutputStream(local), sftp.getSubsystem().getLocalMaxPacketSize());
            rf.close();
        }
        
        private void downloadDir(RemoteResourceInfo remote, File local) throws IOException
        {
            local = FileTransferUtil.getTargetDirectory(local, remote.getName());
            setAttributes(remote, local);
            RemoteDir rd = sftp.openDir(remote.getPath());
            for (RemoteResourceInfo rri : rd.scan(filter))
                download(rri, new File(local.getPath(), rri.getName()));
            rd.close();
        }
        
        void download(RemoteResourceInfo remote, File local) throws IOException
        {
            log.info("Downloading [{}] to [{}]", remote, local);
            if (remote.isDirectory())
                downloadDir(remote, local);
            else if (remote.isRegularFile())
                downloadFile(remote, local);
            else
                throw new IOException(remote + " is not a regular file or directory");
        }
    }

    private class Uploader
    {
        
        private final ModeGetter modeGetter;
        private final FileFilter filter;
        
        Uploader(ModeGetter modeGetter, FileFilter filter)
        {
            this.modeGetter = modeGetter;
            this.filter = filter;
        }
        
        public FileAttributes getAttributes(File local) throws IOException
        {
            FileAttributes.Builder builder = new FileAttributes.Builder().withPermissions(modeGetter
                    .getPermissions(local));
            if (modeGetter.preservesTimes())
                builder.withAtimeMtime(modeGetter.getLastAccessTime(local), modeGetter.getLastModifiedTime(local));
            return builder.build();
        }
        
        // tread carefully
        private void setAttributes(FileAttributes current, File local, String remote) throws IOException
        {
            FileAttributes attrs = getAttributes(local);
            if (current != null
                    && current.getMode().getPermissionsMask() == attrs.getMode().getPermissionsMask()
                    && (!modeGetter.preservesTimes() || (attrs.getAtime() == current.getAtime() && attrs.getMtime() == current
                            .getMtime())))
                return;
            else
                sftp.setAttributes(remote, attrs);
        }
        
        private String prepareDir(File local, String remote) throws IOException
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
                    sftp.makeDir(remote, getAttributes(local));
                    return remote;
                } else
                    throw e;
            }
            
            if (attrs.getMode().getType() == FileMode.Type.DIRECTORY)
                if (pathUtil.getComponents(remote).getName().equals(local.getName()))
                {
                    log.debug("probeDir: {} already exists", remote);
                    setAttributes(attrs, local, remote);
                    return remote;
                } else
                {
                    log.debug("probeDir: {} already exists, path adjusted for {}", remote, local.getName());
                    return prepareDir(local, PathUtil.adjustForParent(remote, local.getName()));
                }
            else
                throw new IOException(attrs.getMode().getType() + " file already exists at " + remote);
        }
        
        private String prepareFile(File local, String remote) throws IOException
        {
            FileAttributes attrs = null;
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
                log.debug("probeFile: {} was directory, path adjusted for {}", remote, local.getName());
                remote = PathUtil.adjustForParent(remote, local.getName());
                return remote;
            } else
            {
                log.debug("probeFile: {} is a {} file that will be replaced", remote, attrs.getMode().getType());
                return remote;
            }
        }
        
        private void uploadDir(File local, String remote) throws IOException
        {
            final String adjusted = prepareDir(local, remote);
            for (File f : local.listFiles(filter))
                upload(f, adjusted);
        }
        
        private void uploadFile(File local, String remote) throws IOException
        {
            final String adjusted = prepareFile(local, remote);
            RemoteFile rf = sftp.open(adjusted, EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.TRUNC),
                    getAttributes(local));
            try
            {
                Pipe.pipe(new FileInputStream(local), rf.getOutputStream(), sftp.getSubsystem()
                        .getRemoteMaxPacketSize()
                        - rf.getOutgoingPacketOverhead());
            } finally
            {
                IOUtils.closeQuietly(rf);
            }
        }
        
        void upload(File local, String remote) throws IOException
        {
            log.info("Uploading [{}] to [{}]", local, remote);
            if (local.isDirectory())
                uploadDir(local, remote);
            else if (local.isFile())
                uploadFile(local, remote);
            else
                throw new IOException(local + " is not a file or directory");
        }
    }
    
}
