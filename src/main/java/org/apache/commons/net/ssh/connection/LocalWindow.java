package org.apache.commons.net.ssh.connection;

import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Constants.Message;

public class LocalWindow extends Window
{
    
    LocalWindow(Channel chan)
    {
        super(chan, true);
    }
    
    /*
     * This is slightly confusing and not sure I completely understand but works well
     */
    public synchronized void check(int max) throws TransportException
    {
        int threshold = Math.min(maxPacketSize * 8, max / 4);
        int diff = max - size;
        if (diff > maxPacketSize && (diff > threshold || size < threshold))
            growBy(diff);
    }
    
    protected synchronized void growBy(int inc) throws TransportException
    {
        sendWindowAdjust(inc);
        expand(inc);
    }
    
    protected synchronized void sendWindowAdjust(int inc) throws TransportException
    {
        log.info("Sending SSH_MSG_CHANNEL_WINDOW_ADJUST to #{} for {} bytes", chan.getRecipient(), inc);
        chan.getTransport().writePacket(new Buffer(Message.CHANNEL_WINDOW_ADJUST) //
                                                                                 .putInt(chan.getRecipient()) //
                                                                                 .putInt(inc));
    }
    
}
