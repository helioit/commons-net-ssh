package org.apache.commons.net.ssh.connection;

import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Constants.Message;

// TODO: move channel opening to ConnProto
public abstract class AbstractDirectChannel extends AbstractChannel implements Channel.Direct
{
    
    protected AbstractDirectChannel(String name, ConnectionService conn)
    {
        super(name, conn);
        conn.attach(this);
    }
    
    public void open() throws ConnectionException, TransportException
    {
        lock.lock();
        try {
            if (!open.isSet()) {
                trans.writePacket(buildOpenReq());
                open.await(conn.getTimeout());
            }
        } finally {
            lock.unlock();
        }
    }
    
    protected Buffer buildOpenReq()
    {
        return new Buffer(Message.CHANNEL_OPEN) //
                                               .putString(type) //
                                               .putInt(id) //
                                               .putInt(lwin.getSize()) //
                                               .putInt(lwin.getMaxPacketSize());
    }
    
    @Override
    protected void gotUnknown(Message cmd, Buffer buf) throws TransportException
    {
        if (cmd == Message.CHANNEL_OPEN_CONFIRMATION) {
            init(buf.getInt(), buf.getInt(), buf.getInt());
            open.set();
        } else if (cmd == Message.CHANNEL_OPEN_FAILURE) {
            open.error(new OpenFailException(type, buf.getInt(), buf.getString()));
            conn.forget(this);
        } else
            super.gotUnknown(cmd, buf);
    }
    
}
