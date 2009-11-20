package org.apache.commons.net.ssh.sftp;

public class RemoteResourceInfo
{
    
    private final String name;
    private final String longName;
    private final FileAttributes attrs;
    
    public RemoteResourceInfo(String name, String longName, FileAttributes attrs)
    {
        this.name = name;
        this.longName = longName;
        this.attrs = attrs;
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
