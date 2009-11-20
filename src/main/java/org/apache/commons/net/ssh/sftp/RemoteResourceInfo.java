package org.apache.commons.net.ssh.sftp;

public class RemoteResourceInfo
{
    
    private final String name;
    private final String longName;
    private final FileAttributes attrs;
    
    public RemoteResourceInfo(Response res)
    {
        this.name = res.readString();
        this.longName = res.readString();
        this.attrs = res.readFileAttributes();
    }
    
    public String getName()
    {
        return name;
    }
    
    public String getLongName()
    {
        return longName;
    }
    
    public FileAttributes getAttributes()
    {
        return attrs;
    }
    
    public boolean isType(FileMode.Type type)
    {
        return attrs.getType() == type;
    }
    
    public boolean isRegularFile()
    {
        return isType(FileMode.Type.REGULAR);
    }
    
    public boolean isDirectory()
    {
        return isType(FileMode.Type.DIRECTORY);
    }
    
    public boolean isSymlink()
    {
        return isType(FileMode.Type.SYMKLINK);
    }
    
}
