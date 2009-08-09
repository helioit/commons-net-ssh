/**
 * 
 */
package org.apache.commons.net.ssh.connection;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.commons.net.ssh.util.Pipe;
import org.apache.commons.net.ssh.util.Pipe.ErrorCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketForwardingConnectListener implements ConnectListener
{
    
    protected final SocketAddress addr;
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    public SocketForwardingConnectListener(SocketAddress addr)
    {
        this.addr = addr;
    }
    
    public void gotConnect(Channel.Forwarded chan) throws IOException
    {
        log.info("New connection from " + chan.getOriginatorIP() + ":" + chan.getOriginatorPort());
        
        Socket sock = new Socket();
        sock.connect(addr);
        
        // ok so far -- could connect, let's confirm the channel
        chan.confirm();
        
        ErrorCallback chanCloser = Pipe.closeOnErrorCallback(chan);
        
        new Pipe("soc2chan", sock.getInputStream(), chan.getOutputStream()) //
                                                                           .bufSize(chan.getRemoteMaxPacketSize()) //
                                                                           .closeOutputStreamOnEOF(true) //
                                                                           .errorCallback(chanCloser) //
                                                                           .daemon(true) //
                                                                           .start();
        
        new Pipe("chan2soc", chan.getInputStream(), sock.getOutputStream()) //
                                                                           .bufSize(chan.getLocalMaxPacketSize()) //                                                       
                                                                           .closeOutputStreamOnEOF(true) //
                                                                           .errorCallback(chanCloser) //
                                                                           .daemon(true) //
                                                                           .start();
    }
    
}