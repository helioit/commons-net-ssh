package org.apache.commons.net.ssh.connection;

public class RemoteWindow extends Window
{
    
    public RemoteWindow(Channel chan)
    {
        super(chan, false);
    }
    
    public synchronized void waitAndConsume(int howMuch) throws ConnectionException
    {
        while (size < howMuch) {
            log.debug("Waiting, need window space for {} bytes", howMuch);
            try {
                wait();
            } catch (InterruptedException ie) {
                throw new ConnectionException(ie);
            }
        }
        consume(howMuch);
    }
    
}
