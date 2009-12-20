package org.apache.commons.net.ssh.transport;

import org.apache.commons.net.ssh.SSHPacket;
import org.apache.commons.net.ssh.util.Constants.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Heartbeater extends Thread
{
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final TransportProtocol trans;
    
    private int interval = 0;
    
    Heartbeater(TransportProtocol trans)
    {
        this.trans = trans;
        setName("heartbeater");
    }
    
    synchronized void setInterval(int interval)
    {
        this.interval = interval;
        if (interval != 0)
        {
            start();
            notify();
        }
    }
    
    synchronized int getInterval()
    {
        return interval;
    }
    
    @Override
    public void run()
    {
        try
        {
            while (!Thread.currentThread().isInterrupted())
            {
                int hi;
                synchronized (this)
                {
                    while ((hi = interval) == 0)
                        wait();
                }
                if (trans.isRunning())
                {
                    log.info("Sending heartbeat since {} seconds elapsed", hi);
                    trans.write(new SSHPacket(Message.IGNORE));
                }
                Thread.sleep(hi * 1000);
            }
        } catch (Exception e)
        {
            if (Thread.currentThread().isInterrupted())
            {
                // We are meant to shut up and draw to a close if interrupted
            } else
                trans.die(e);
        }
        
        log.debug("Stopping");
    }
    
}
