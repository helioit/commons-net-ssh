/*
 * Licensed to the Apache Software Founation (ASF) under one
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
package org.apache.commons.net.ssh.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.commons.net.ssh.AbstractService;
import org.apache.commons.net.ssh.Config;
import org.apache.commons.net.ssh.ErrorNotifiable;
import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.Service;
import org.apache.commons.net.ssh.cipher.Cipher;
import org.apache.commons.net.ssh.compression.Compression;
import org.apache.commons.net.ssh.mac.MAC;
import org.apache.commons.net.ssh.random.Random;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Event;
import org.apache.commons.net.ssh.util.IOUtils;
import org.apache.commons.net.ssh.util.Constants.DisconnectReason;
import org.apache.commons.net.ssh.util.Constants.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A thread-safe {@link Transport} implementation.
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public final class TransportProtocol implements Transport
{
    
    private static class NullService extends AbstractService
    {
        public NullService(Transport trans)
        {
            super("null-service", trans);
        }
    }
    
    private final Thread dispatcher = new Thread()
        {
            {
                setName("dispatcher");
                //setDaemon(true);
            }
            
            @Override
            public void run()
            {
                try {
                    byte[] recvbuf = new byte[decoder.getMaxPacketLength()];
                    int needed = 1;
                    int read;
                    while (!Thread.currentThread().isInterrupted()) {
                        read = input.read(recvbuf, 0, needed);
                        if (read == -1)
                            throw new TransportException("Broken transport; encountered EOF");
                        else
                            needed = decoder.received(recvbuf, read);
                    }
                } catch (Exception e) {
                    if (Thread.currentThread().isInterrupted()) {
                        // We are meant to shut up and draw to a close if interrupted
                    } else
                        die(e);
                }
                log.debug("Stopping");
            }
        };
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final Config config;
    private final Service nullService = new NullService(this);
    
    /** Currently active service e.g. UserAuthService, ConnectionService */
    private volatile Service service = nullService;
    
    private Socket socket;
    private InputStream input;
    private OutputStream output;
    
    /** Psuedo-random number generator as retrieved from the factory manager */
    private final Random prng;
    
    /** For key (re)exchange */
    private final KeyExchanger kexer;
    /** For encoding packets */
    private final Encoder encoder;
    /** For decoding packets */
    private final Decoder decoder;
    
    /** Message identifier of last packet received */
    private Message msg;
    
    /** Client version identification string */
    private final String clientID;
    
    /** Server version identification string */
    private String serverID;
    
    private final Event<TransportException> serviceAccept =
            new Event<TransportException>("service accept", TransportException.chainer);
    
    private final Event<TransportException> close =
            new Event<TransportException>("transport close", TransportException.chainer);
    
    private volatile int timeout = 30;
    
    private volatile boolean authed;
    
    private String hostname;
    
    public TransportProtocol(Config config)
    {
        this.config = config;
        this.prng = config.getRandomFactory().create();
        this.kexer = new KeyExchanger(this);
        this.encoder = new Encoder(prng);
        this.decoder = new Decoder(this);
        clientID = "SSH-2.0-" + config.getVersion();
    }
    
    public void disconnect()
    {
        disconnect(DisconnectReason.BY_APPLICATION);
    }
    
    public void disconnect(DisconnectReason reason)
    {
        disconnect(reason, "");
    }
    
    public void disconnect(DisconnectReason reason, String message)
    {
        close.lock();
        try {
            if (!close.isSet()) {
                sendDisconnect(reason, message);
                finishOff();
                close.set();
            }
        } finally {
            close.unlock();
        }
    }
    
    public String getClientVersion()
    {
        return clientID.substring(8);
    }
    
    public Config getConfig()
    {
        return config;
    }
    
    public KeyExchanger getKeyExchanger()
    {
        return kexer;
    }
    
    public String getRemoteHost()
    {
        return hostname;
    }
    
    public int getRemotePort()
    {
        return socket.getPort();
    }
    
    public String getServerVersion()
    {
        return serverID == null ? serverID : serverID.substring(8);
    }
    
    public Service getService()
    {
        return service;
    }
    
    public byte[] getSessionID()
    {
        return kexer.getSessionID();
    }
    
    public int getTimeout()
    {
        return timeout;
    }
    
    /**
     * This is where all incoming packets are handled. If they pertain to the transport layer, they
     * are handled here; otherwise they are delegated to the active service instance if any via
     * {@link Service#handle}.
     * <p>
     * Even among the transport layer specific packets, key exchange packets are delegated to
     * {@link KeyExchanger#handle}.
     * <p>
     * This method is called in the context of the {@link #dispatcher} thread via
     * {@link Decoder#received} when a full packet has been decoded.
     * 
     * @param msg
     *            the message identifer
     * @param buf
     *            buffer containg rest of the packet
     * @throws SSHException
     *             if an error occurs during handling (unrecoverable)
     */
    public void handle(Message msg, Buffer buf) throws SSHException
    {
        this.msg = msg;
        
        log.trace("Received packet {}", msg);
        
        if (msg.geq(50)) // not a transport layer packet
            service.handle(msg, buf);
        
        else if (msg.in(20, 21) || msg.in(30, 49)) // kex packet
            kexer.handle(msg, buf);
        
        else
            switch (msg)
            {
            case DISCONNECT:
            {
                gotDisconnect(buf);
            }
            case IGNORE:
            {
                log.info("Received SSH_MSG_IGNORE");
                break;
            }
            case UNIMPLEMENTED:
            {
                gotUnimplemented(buf);
                break;
            }
            case DEBUG:
            {
                gotDebug(buf);
                break;
            }
            case SERVICE_ACCEPT:
            {
                gotServiceAccept();
                break;
            }
            default:
                sendUnimplemented();
            }
    }
    
    public void init(String hostname, Socket socket) throws TransportException
    {
        this.hostname = hostname == null ? socket.getInetAddress().getHostName() : hostname;
        this.socket = socket;
        
        try {
            
            input = socket.getInputStream();
            output = socket.getOutputStream();
            
            log.info("Client identity string: {}", clientID);
            output.write((clientID + "\r\n").getBytes());
            
            // Read server's ID
            Buffer buf = new Buffer();
            while ((serverID = readIdentification(buf)) == null)
                buf.putByte((byte) input.read());
            
            log.info("Server identity string: {}", serverID);
            
        } catch (IOException e) {
            throw new TransportException(e);
        }
        
        dispatcher.start();
    }
    
    public boolean isAuthenticated()
    {
        return authed;
    }
    
    public boolean isRunning()
    {
        return !close.isSet();
    }
    
    public void join() throws TransportException
    {
        close.await();
    }
    
    public synchronized void reqService(Service service) throws TransportException
    {
        serviceAccept.clear();
        sendServiceRequest(service.getName());
        serviceAccept.await(timeout);
        setService(service);
    }
    
    public long sendUnimplemented() throws TransportException
    {
        long seq = decoder.getSequenceNumber();
        log.info("Sending SSH_MSG_UNIMPLEMENTED for packet #{}", seq);
        return writePacket(new Buffer(Message.UNIMPLEMENTED).putInt(seq));
    }
    
    public void setAuthenticated()
    {
        this.authed = true;
        encoder.setAuthenticated();
        decoder.setAuthenticated();
    }
    
    public synchronized void setService(Service service)
    {
        if (service == null)
            service = nullService;
        
        log.info("Setting active service to {}", service.getName());
        this.service = service;
    }
    
    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }
    
    public long writePacket(Buffer payload) throws TransportException
    {
        synchronized (encoder) {
            
            if (kexer.isKexOngoing()) {
                // Only transport layer packets (1 to 49) allowed except SERVICE_REQUEST
                Message m = Message.fromByte(payload.array()[payload.rpos()]);
                if (!m.in(1, 49) || m == Message.SERVICE_REQUEST)
                    kexer.waitForDone();
            } else if (encoder.getSequenceNumber() == 0) // We get here every 2**32th packet
                kexer.startKex(true);
            
            long seq = encoder.encode(payload);
            try {
                output.write(payload.array(), payload.rpos(), payload.available());
                output.flush();
            } catch (IOException ioe) {
                throw new TransportException(ioe);
            }
            return seq;
            
        }
    }
    
    private void die(Exception ex)
    {
        close.lock();
        try {
            if (!close.isSet()) {
                
                log.error("Dying because - {}", ex.toString());
                
                SSHException causeOfDeath = SSHException.chainer.chain(ex);
                
                ErrorNotifiable.Util.alertAll(causeOfDeath, close, serviceAccept, kexer);
                service.notifyError(causeOfDeath);
                service = nullService;
                
                { // Perhaps can send disconnect packet to server
                    final boolean didNotReceiveDisconnect = msg != Message.DISCONNECT;
                    final boolean gotRequiredInfo = causeOfDeath.getDisconnectReason() != DisconnectReason.UNKNOWN;
                    if (didNotReceiveDisconnect && gotRequiredInfo)
                        sendDisconnect(causeOfDeath.getDisconnectReason(), causeOfDeath.getMessage());
                }
                
                finishOff();
                
                close.set();
            }
        } finally {
            close.unlock();
        }
    }
    
    private void finishOff()
    {
        dispatcher.interrupt();
        try {
            socket.shutdownInput();
        } catch (IOException ignore) {
        }
        
        try {
            socket.shutdownOutput();
        } catch (IOException ignore) {
        }
    }
    
    private void gotDebug(Buffer buf)
    {
        boolean display = buf.getBoolean();
        String message = buf.getString();
        log.info("Received SSH_MSG_DEBUG (display={}) '{}'", display, message);
    }
    
    private void gotDisconnect(Buffer buf) throws TransportException
    {
        DisconnectReason code = DisconnectReason.fromInt(buf.getInt());
        String message = buf.getString();
        log.info("Received SSH_MSG_DISCONNECT (reason={}, msg={})", code, message);
        throw new TransportException(code, "Disconnected; server said: " + message);
    }
    
    private void gotServiceAccept() throws TransportException
    {
        if (!serviceAccept.hasWaiters())
            throw new TransportException(DisconnectReason.PROTOCOL_ERROR,
                                         "Got a service accept notification when none was awaited");
        serviceAccept.set();
    }
    
    /**
     * Got an SSH_MSG_UNIMPLEMENTED, so lets see where we're at and act accordingly.
     * 
     * @param seqNum
     * @throws TransportException
     */
    private void gotUnimplemented(Buffer buf) throws SSHException
    {
        long seqNum = buf.getLong();
        log.info("Received SSH_MSG_UNIMPLEMENTED #{}", seqNum);
        if (kexer.isKexOngoing())
            throw new TransportException("Received SSH_MSG_UNIMPLEMENTED while exchanging keys");
        getService().notifyUnimplemented(seqNum);
    }
    
    /**
     * Reads the identification string from the SSH server. This is the very first string that is
     * sent upon connection by the server. It takes the form of, e.g. "SSH-2.0-OpenSSH_ver".
     * <p>
     * Several concerns are taken care of here, e.g. verifying protocol version, correct line
     * endings as specified in RFC and such.
     * <p>
     * It should be called from a loop like {@code String id; while ((id = readIdentification) ==
     * null) ; }
     * <p>
     * This is not effcient but is only done once.
     * 
     * @param buffer
     * @return
     * @throws IOException
     */
    private String readIdentification(Buffer buffer) throws IOException
    {
        String ident;
        
        byte[] data = new byte[256];
        for (;;) {
            int savedBufPos = buffer.rpos();
            int pos = 0;
            boolean needLF = false;
            for (;;) {
                if (buffer.available() == 0) {
                    // Need more data, so undo reading and return null
                    buffer.rpos(savedBufPos);
                    return null;
                }
                byte b = buffer.getByte();
                if (b == '\r') {
                    needLF = true;
                    continue;
                }
                if (b == '\n')
                    break;
                if (needLF)
                    throw new TransportException("Incorrect identification: bad line ending");
                if (pos >= data.length)
                    throw new TransportException("Incorrect identification: line too long");
                data[pos++] = b;
            }
            ident = new String(data, 0, pos);
            if (ident.startsWith("SSH-"))
                break;
            if (buffer.rpos() > 16 * 1024)
                throw new TransportException("Incorrect identification: too many header lines");
        }
        
        if (!ident.startsWith("SSH-2.0-") && !ident.startsWith("SSH-1.99-"))
            throw new TransportException(DisconnectReason.PROTOCOL_VERSION_NOT_SUPPORTED,
                                         "Server does not support SSHv2, identified as: " + ident);
        
        return ident;
    }
    
    private void sendDisconnect(DisconnectReason reason, String message)
    {
        if (message == null)
            message = "";
        log.debug("Sending SSH_MSG_DISCONNECT: reason=[{}], msg=[{}]", reason, message);
        IOUtils.writeQuietly(this, new Buffer(Message.DISCONNECT) //
                                                                 .putInt(reason.toInt()) //
                                                                 .putString(message) //                                                                             
                                                                 .putString("")); // lang tag   
    }
    
    /**
     * Sends a service request for the specified service
     * 
     * @param serviceName
     *            name of the service being requested
     * @throws TransportException
     *             if there is an error while sending the request
     */
    private void sendServiceRequest(String serviceName) throws TransportException
    {
        log.debug("Sending SSH_MSG_SERVICE_REQUEST for {}", serviceName);
        writePacket(new Buffer(Message.SERVICE_REQUEST).putString(serviceName));
    }
    
    String getClientID()
    {
        return clientID;
    }
    
    String getServerID()
    {
        return serverID;
    }
    
    void setClientToServerAlgorithms(Cipher cipher, MAC mac, Compression comp)
    {
        encoder.setAlgorithms(cipher, mac, comp);
    }
    
    void setServerToClientAlgorithms(Cipher cipher, MAC mac, Compression comp)
    {
        decoder.setAlgorithms(cipher, mac, comp);
    }
    
}
