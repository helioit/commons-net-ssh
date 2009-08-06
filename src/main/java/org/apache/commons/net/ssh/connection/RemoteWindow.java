package org.apache.commons.net.ssh.connection;

public class RemoteWindow extends Window
{
    
    public RemoteWindow(Channel chan)
    {
        super(chan, false);
    }
    
    public synchronized void waitAndConsume(int howMuch) throws InterruptedException
    {
        while (size < howMuch) {
            log.debug("Waiting, need window space for {} bytes", howMuch);
            wait();
        }
        consume(howMuch);
    }
    
}
