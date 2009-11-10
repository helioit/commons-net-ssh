package org.apache.commons.net.ssh.sftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ssh.sftp.Response.StatusCode;

public class RemoteFile
{
    
    private final SFTP sftp;
    private final String handle;
    
    public RemoteFile(SFTP sftp, String handle)
    {
        this.sftp = sftp;
        this.handle = handle;
    }
    
    public void close() throws IOException
    {
        Request req = newRequest(PacketType.CLOSE);
        
        sftp.send(req);
        
        req.getFuture().get(sftp.timeout).ensureStatus(StatusCode.OK);
    }
    
    public InputStream getInputStream()
    {
        return new RemoteFileInputStream(this);
    }
    
    public OutputStream getOutputStream()
    {
        return new RemoteFileOutputStream(this);
    }
    
    public int read(long fileOffset, byte[] buf, int offset, int len) throws IOException
    {
        Request req = newRequest(PacketType.READ);
        req.putUINT64(offset);
        req.putInt(len);
        
        sftp.send(req);
        
        Response res = req.getFuture().get(sftp.timeout);
        
        switch (res.getType())
        {
        case DATA:
            int recvLen = res.readInt();
            System.arraycopy(res.array(), res.rpos(), buf, offset, recvLen);
            return recvLen;
        case STATUS:
            res.ensureStatus(StatusCode.EOF);
            return -1;
        default:
            throw new SFTPException("Unexpected packet: " + res.getType());
        }
    }
    
    private Request newRequest(PacketType type)
    {
        Request req = sftp.newRequest(type);
        req.putString(handle);
        return req;
    }
    
    public FileAttributes getFileAttributes() throws IOException
    {
        Request req = newRequest(PacketType.FSTAT);
        
        sftp.send(req);
        
        Response res = req.getFuture().get(sftp.timeout);
        res.ensureStatus(StatusCode.OK);
        return res.readFileAttributes();
    }
    
    public void write(long fileOffset, byte[] data, int off, int len) throws IOException
    {
        Request req = newRequest(PacketType.WRITE);
        req.putUINT64(fileOffset);
        req.putString(data, off, len);
        
        sftp.send(req);
        
        Response res = req.getFuture().get(sftp.timeout);
        res.ensureStatus(StatusCode.OK);
    }
    
}
