package org.apache.commons.net.ssh.connection;

import org.apache.commons.net.ssh.connection.OpenFailException.Reason;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Constants.Message;

//TODO: move to ConnProto
public abstract class AbstractForwardedChannel extends AbstractChannel implements Channel.Forwarded
{
    
    protected final String origIP;
    protected final int origPort;
    
    protected AbstractForwardedChannel(String name, Connection conn, int recipient, int remoteWinSize,
            int remoteMaxPacketSize, String origIP, int origPort)
    {
        super(name, conn);
        this.origIP = origIP;
        this.origPort = origPort;
        init(recipient, remoteWinSize, remoteMaxPacketSize);
    }
    
    public void confirm() throws TransportException
    {
        log.info("Confirming `{}` channel #{}", type, id);
        trans.writePacket(newBuffer(Message.CHANNEL_OPEN_CONFIRMATION) //
                                                                      .putInt(id) //
                                                                      .putInt(lwin.getSize()) //
                                                                      .putInt(lwin.getMaxPacketSize()));
        open.set();
        conn.attach(this);
    }
    
    public String getOriginatorIP()
    {
        return origIP;
    }
    
    public int getOriginatorPort()
    {
        return origPort;
    }
    
    public void reject(Reason reason, String message) throws TransportException
    {
        log.info("Rejecting `{}` channel: {}", type, message);
        trans.writePacket(new Buffer(Message.CHANNEL_OPEN_FAILURE) //
                                                                  .putInt(reason.getCode()) //
                                                                  .putString(message));
    }
    
}
