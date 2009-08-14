/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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

/**
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class LocalPortForwarder
{
    
    protected class DirectTCPIPChannel extends AbstractDirectChannel
    {
        
        protected final Socket sock;
        
        protected DirectTCPIPChannel(Connection conn, Socket sock)
        {
            super("direct-tcpip", conn);
            this.sock = sock;
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
        
        protected void start() throws IOException
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
        
    }
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final Connection conn;
    protected final Event<ConnectionException> close =
            new Event<ConnectionException>("pfd close", ConnectionException.chainer);
    protected final ServerSocket ss;
    protected final String host;
    protected final int port;
    
    protected final Thread listener = new Thread()
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
    
    /**
     * Create a local port forwarder with specified binding ({@code listeningAddr}. It does not,
     * however, start listening unless {@link #startListening() explicitly told to}.
     * 
     * @param conn
     *            {@link Connection} implementation
     * @param listeningAddr
     *            {@link SocketAddress} this forwarder will listen on, if {@code null} then an
     *            ephemeral port and valid local address will be picked to bind the server socket
     * @param host
     *            what host the SSH server will further forward to
     * @param port
     *            port on {@code toHost}
     * @throws IOException
     *             if there is an error binding on specified {@code listeningAddr}
     */
    public LocalPortForwarder(Connection conn, SocketAddress listeningAddr, String host, int port) throws IOException
    {
        this.conn = conn;
        this.host = host;
        this.port = port;
        this.ss = new ServerSocket();
        ss.setReceiveBufferSize(conn.getMaxPacketSize());
        ss.bind(listeningAddr);
        startListening();
    }
    
    public SocketAddress getListeningAddress()
    {
        return ss.getLocalSocketAddress();
    }
    
    /**
     * Spawns a daemon thread for listening for incoming connections and forwarding to remote host
     * as a channel.
     */
    public void startListening()
    {
        listener.start();
    }
    
    /**
     * Stop this port forwarding.
     */
    public void stopListening()
    {
        listener.interrupt();
        try {
            ss.close(); // in case it is blocked on accept (as it will be...)
        } catch (IOException ignore) {
        }
    }
    
}
