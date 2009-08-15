package org.apache.commons.net.ssh.connection;

import java.io.IOException;

import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public abstract class AbstractForwardedChannelOpener implements ForwardedChannelOpener
{
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final String chanType;
    protected final Connection conn;
    
    protected AbstractForwardedChannelOpener(String chanType, Connection conn)
    {
        this.chanType = chanType;
        this.conn = conn;
    }
    
    // Javadoc in interface
    public String getChannelType()
    {
        return chanType;
    }
    
    /*
     * Calls the listener with the new channel in a separate thread.
     */
    protected void callListener(final ConnectListener listener, final Channel.Forwarded chan)
    {
        new Thread()
            {
                {
                    setName("ConnectListener");
                }
                
                @Override
                public void run()
                {
                    try {
                        listener.gotConnect(chan);
                    } catch (IOException logged) {
                        log.warn("In callback to {}: {}", listener, logged);
                        if (chan.isOpen())
                            IOUtils.closeQuietly(chan);
                        else
                            try {
                                chan.reject(OpenFailException.Reason.CONNECT_FAILED, "");
                            } catch (TransportException cantdonthn) {
                                log.warn("Error rejecting {}: {}", chan, cantdonthn);
                            }
                    }
                }
            }.start();
    }
    
}