package org.apache.commons.net.ssh.sftp;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.net.ssh.SessionFactory;

public class StatefulSFTPClient extends SFTPClient
{
    
    private String cwd;
    
    public StatefulSFTPClient(SessionFactory ssh) throws IOException
    {
        super(ssh);
        this.cwd = getSFTPEngine().canonicalize(".");
        log.info("Start dir = " + cwd);
    }
    
    private synchronized String cwdify(String path)
    {
        return PathUtil.adjustForParent(cwd, path);
    }
    
    public synchronized void cd(String dirname) throws IOException
    {
        cwd = cwdify(dirname);
        log.info("CWD = " + cwd);
    }
    
    public synchronized List<RemoteResourceInfo> ls() throws IOException
    {
        return ls(cwd, null);
    }
    
    public synchronized List<RemoteResourceInfo> ls(RemoteResourceFilter filter) throws IOException
    {
        return ls(cwd, filter);
    }
    
    public synchronized String getcwd() throws IOException
    {
        return super.canonicalize(cwd);
    }
    
    @Override
    public List<RemoteResourceInfo> ls(String path) throws IOException
    {
        return ls(path, null);
    }
    
    @Override
    public List<RemoteResourceInfo> ls(String path, RemoteResourceFilter filter) throws IOException
    {
        RemoteDir dir = getSFTPEngine().openDir(path);
        try
        {
            return dir.scan(filter);
        } finally
        {
            dir.close();
        }
    }
    
    @Override
    public RemoteFile open(String filename, Set<OpenMode> mode) throws IOException
    {
        return super.open(cwdify(filename), mode);
    }
    
    @Override
    public RemoteFile open(String filename) throws IOException
    {
        return super.open(cwdify(filename));
    }
    
    @Override
    public void mkdir(String dirname) throws IOException
    {
        super.mkdir(cwdify(dirname));
    }
    
    @Override
    public void rename(String oldpath, String newpath) throws IOException
    {
        super.rename(cwdify(oldpath), cwdify(newpath));
    }
    
    @Override
    public void rm(String filename) throws IOException
    {
        super.rm(cwdify(filename));
    }
    
    @Override
    public void rmdir(String dirname) throws IOException
    {
        super.rmdir(cwdify(dirname));
    }
    
    @Override
    public void symlink(String linkpath, String targetpath) throws IOException
    {
        super.symlink(cwdify(linkpath), cwdify(targetpath));
    }
    
    @Override
    public void setattr(String path, FileAttributes attrs) throws IOException
    {
        super.setattr(cwdify(path), attrs);
    }
    
    @Override
    public String readlink(String path) throws IOException
    {
        return super.readlink(cwdify(path));
    }
    
    @Override
    public FileAttributes stat(String path) throws IOException
    {
        return super.stat(cwdify(path));
    }
    
    @Override
    public FileAttributes lstat(String path) throws IOException
    {
        return super.lstat(cwdify(path));
    }
    
    @Override
    public void truncate(String path, long size) throws IOException
    {
        super.truncate(cwdify(path), size);
    }
    
    @Override
    public String canonicalize(String path) throws IOException
    {
        return super.canonicalize(cwdify(path));
    }
    
    @Override
    public void get(String source, String dest) throws IOException
    {
        super.get(cwdify(source), dest);
    }
    
    @Override
    public void put(String source, String dest) throws IOException
    {
        super.get(source, cwdify(dest));
    }
    
}
