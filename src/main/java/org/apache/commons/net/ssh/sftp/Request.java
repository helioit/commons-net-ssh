package org.apache.commons.net.ssh.sftp;

import org.apache.commons.net.ssh.util.Future;

public class Request extends Packet
{
    
    private final long reqID;
    private final Future<Response, SFTPException> future;
    
    public Request(PacketType type, long reqID)
    {
        super();
        this.reqID = reqID;
        future = new Future<Response, SFTPException>("sftp / " + reqID, SFTPException.chainer);
        putByte(type.toByte());
        putInt(reqID);
    }
    
    public long readRequestID()
    {
        return reqID;
    }
    
    public Future<Response, SFTPException> getFuture()
    {
        return future;
    }
    
}
