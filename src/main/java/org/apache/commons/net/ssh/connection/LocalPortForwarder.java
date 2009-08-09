package org.apache.commons.net.ssh.connection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Event;
import org.apache.commons.net.ssh.util.Pipe;
import org.apache.commons.net.ssh.util.Pipe.ErrorCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalPortForwarder
{
    
    private class DirectTCPIPChannel extends AbstractDirectChannel
    {
        
        private final Socket sock;
        
        private DirectTCPIPChannel(Connection conn, Socket sock)
        {
            super("direct-tcpip", conn);
            this.sock = sock;
        }
        
        private void start() throws IOException
        {
            sock.setSendBufferSize(getRemoteMaxPacketSize());
            
            ErrorCallback chanCloser = Pipe.closeOnErrorCallback(this);
            
            new Pipe("chan2soc", in, sock.getOutputStream()) //
                                                            .bufSize(getLocalMaxPacketSize()) //
                                                            .closeOutputStreamOnEOF(true) //
                                                            .errorCallback(chanCloser) //
                                                            .daemon(true) //
                                                            .start();
            
            new Pipe("soc2chan", sock.getInputStream(), out) //
                                                            .bufSize(getRemoteMaxPacketSize()) //
                                                            .closeOutputStreamOnEOF(true) //
                                                            .errorCallback(chanCloser) //
                                                            .daemon(true) //
                                                            .start();
            
        }
        
        @Override
        protected Buffer buildOpenReq()
        {
            return super.buildOpenReq() //
                        .putString(host) //
                        .putInt(port) //
                        .putString(ss.getInetAddress().getHostAddress()) //
                        .putInt(ss.getLocalPort());
        }
        
    }
    
    private final Connection conn;
    private final Event<ConnectionException> close =
            new Event<ConnectionException>("pfd close", ConnectionException.chainer);
    private final ServerSocket ss;
    private final String host;
    private final int port;
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final Thread listener = new Thread()
        {
            {
                setName("pfd"); // "port forwarding daemon"
                setDaemon(true);
            }
            
            @Override
            public void run()
            {
                log.info("Listening on {}", ss.getLocalSocketAddress());
                while (!Thread.currentThread().isInterrupted()) {
                    Socket sock;
                    try {
                        sock = ss.accept();
                        log.info("Got connection from {}", sock.getRemoteSocketAddress());
                    } catch (IOException e) {
                        if (!Thread.currentThread().isInterrupted())
                            close.error(e);
                        break;
                    }
                    try {
                        DirectTCPIPChannel chan = new DirectTCPIPChannel(conn, sock);
                        chan.open();
                        chan.start();
                    } catch (IOException justLog) {
                        log.error("While initializing direct-tcpip channel from {}: {}", sock.getRemoteSocketAddress(),
                                  justLog.toString());
                    }
                }
                close.set();
            }
        };
    
    public LocalPortForwarder(Connection conn, SocketAddress listeningAddr, String toHost, int toPort)
            throws IOException
    {
        this.conn = conn;
        this.host = toHost;
        this.port = toPort;
        this.ss = new ServerSocket();
        ss.setReceiveBufferSize(conn.getMaxPacketSize());
        ss.bind(listeningAddr);
    }
    
    public void startListening()
    {
        listener.start();
    }
    
    public void stopListening()
    {
        listener.interrupt();
        close();
    }
    
    protected void close()
    {
        try {
            ss.close(); // in case it is blocked on accept (as it will be...)
        } catch (IOException ignore) {
        }
    }
    
}
