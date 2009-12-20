package org.apache.commons.net.ssh.transport;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Reader extends Thread
{
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final TransportProtocol trans;
    
    Reader(TransportProtocol trans)
    {
        this.trans = trans;
        setName("reader");
    }
    
    @Override
    public void run()
    {
        final Thread curThread = Thread.currentThread();
        
        try
        {
            
            final Decoder decoder = trans.getDecoder();
            final InputStream inp = trans.getConnInfo().getInputStream();
            
            final byte[] recvbuf = new byte[decoder.getMaxPacketLength()];
            
            int needed = 1;
            
            while (!curThread.isInterrupted())
            {
                int read = inp.read(recvbuf, 0, needed);
                if (read == -1)
                    throw new TransportException("Broken transport; encountered EOF");
                else
                    needed = decoder.received(recvbuf, read);
            }
            
        } catch (Exception e)
        {
            if (curThread.isInterrupted())
            {
                // We are meant to shut up and draw to a close if interrupted
            } else
                trans.die(e);
        }
        
        log.debug("Stopping");
    }
    
}
