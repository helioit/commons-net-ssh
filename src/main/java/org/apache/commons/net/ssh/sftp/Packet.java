package org.apache.commons.net.ssh.sftp;

import org.apache.commons.net.ssh.util.Buffer;

public class Packet extends Buffer
{
    
    public Packet()
    {
        super();
    }
    
    public Packet(Packet pk)
    {
        super(pk);
    }
    
    public FileAttributes readFileAttributes()
    {
        return new FileAttributes(this);
    }
    
    public Packet putFileAttributes(FileAttributes fa)
    {
        return (Packet) putBuffer(fa.toBuffer());
    }
    
    public PacketType readType()
    {
        return PacketType.fromByte(readByte());
    }
    
    public Packet putType(PacketType type)
    {
        return (Packet) putByte(type.toByte());
    }
    
}
