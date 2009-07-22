package org.apache.commons.net.ssh.connection;

public class RemoteWindow extends Window
{
    
    public synchronized void waitAndConsume(int howMuch) throws InterruptedException
    {
        while (size < howMuch) {
            log.debug("Waiting for {} bytes", howMuch);
            wait();
        }
        consume(howMuch);
    }
    
}