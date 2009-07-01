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
package org.apache.commons.net.ssh.transport;

import static org.apache.commons.net.ssh.Constants.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.net.ssh.FactoryManager;
import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.Service;
import org.apache.commons.net.ssh.Constants.DisconnectReason;
import org.apache.commons.net.ssh.Constants.Message;
import org.apache.commons.net.ssh.random.Random;
import org.apache.commons.net.ssh.util.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Thread-safe {@link Session} implementation.
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class Transport implements Session
{
    
    /*
     * This enum describes what state this SSH session is in, so we that we know which class to
     * delegate message handling to, can wait for some state to be reached, etc.
     */
    private enum State
    {
        KEX, // delegate message handling to KexHandler
        KEX_DONE, // indicates kex done
        SERVICE_REQ, // a service has been requested
        SERVICE, // service request was successful; delegate handling to active service
        ERROR, // indicates an error event in one of the threads
        STOPPED, // indicates this session has been stopped
    }
    
    private State state;
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final FactoryManager fm;
    
    /** Psuedo-random number generator as retrieved from the factory manager */
    final Random prng;
    
    /** For key (re)exchange */
    private final KexHandler kex;
    
    /** For encoding and decoding SSH packets */
    final EncDec bin;
    
    /**
     * Outgoing data is put here. Since it is a SynchronousQueue, until {@link #outPump} is
     * interested in taking, putting an item blocks.
     */
    private final SynchronousQueue<byte[]> outQ = new SynchronousQueue<byte[]>();
    
    /**
     * If an error occurs in one of the threads spawned by this class, it is set in this field.
     */
    private Exception exception;
    
    /** Lock for session state */
    private final ReentrantLock stateLock = new ReentrantLock();
    private final Condition stateChange = stateLock.newCondition();
    
    /** Lock supporting correct encoding and queuing of packets */
    final ReentrantLock writeLock = new ReentrantLock();
    
    private InputStream input;
    private OutputStream output;
    
    /**
     * This thread reads data byte-by-byte from the input stream, passing it on to
     * {@link EncDec#gotByte(byte)} and this may result in a callback to {@link #handle(Buffer)}
     * when a full packet has been decoded. Thus a lot happens in this thread's context.
     */
    private final Thread inPump = new Thread()
    {
        {
            setDaemon(true);
            setName("inPump");
        }
        
        @Override
        public void run()
        {
            while (!stopPumping)
                try {
                    bin.gotByte((byte) input.read());
                } catch (Exception e) {
                    if (!stopPumping) {
                        if (outPump.isAlive() && e instanceof SSHException) {
                            /*
                             * send SSH_MSG_DISCONNECT if we have the required info in the exception
                             * and if outPump is up for it
                             */
                            DisconnectReason reason = ((SSHException) e).getDisconnectReason();
                            if (!reason.equals(DisconnectReason.UNKNOWN))
                                disconnect(reason, ((SSHException) e).getMessage());
                        }
                        setError(e);
                    }
                }
            log.debug("Stopping");
        }
    };
    
    /**
     * This thread waits for {@link #outQ} to offer some byte[] and sends the deliciousness over the
     * output stream for this session.
     * <p>
     * Runs so long as it's not told to {@link #stopPumping} or an Exception is caught. In the
     * latter case, it calls {@link #setError(Exception)}
     */
    private final Thread outPump = new Thread()
    {
        {
            setDaemon(true);
            setName("outPump");
        }
        
        @Override
        public void run()
        {
            while (!stopPumping)
                try {
                    output.write(outQ.take());
                } catch (Exception e) {
                    if (!stopPumping)
                        setError(e);
                }
            log.debug("Stopping");
        }
    };
    
    /** True value tells inPump and outPump to stop */
    private volatile boolean stopPumping = false;
    
    private Service service; // currently active service i.e. ssh-userauth, ssh-connection
    
    boolean authed = false;
    
    /** Client version identification string */
    String clientID = "SSH-2.0-" + VERSION;
    
    /** Server version identification string */
    String serverID;
    
    /**
     * {@link HostKeyVerifier#verify(InetAddress, PublicKey)} is invoked by
     * {@link #verifyHost(PublicKey)} when we are ready to verify the the server's host key.
     */
    private final Queue<HostKeyVerifier> hkvs = new LinkedList<HostKeyVerifier>();
    
    private Socket socket;
    
    Semaphore kexSem = new Semaphore(1);
    
    public Transport(FactoryManager factoryManager)
    {
        assert factoryManager != null;
        fm = factoryManager;
        prng = factoryManager.getRandomFactory().create();
        bin = new EncDec(this);
        kex = new KexHandler(this);
    }
    
    public synchronized void addHostKeyVerifier(HostKeyVerifier hkv)
    {
        hkvs.add(hkv);
    }
    
    public void disconnect()
    {
        disconnect(DisconnectReason.BY_APPLICATION);
    }
    
    public void disconnect(DisconnectReason reason)
    {
        disconnect(reason, "");
    }
    
    public void disconnect(DisconnectReason reason, String msg)
    {
        log.debug("Sending SSH_MSG_DISCONNECT: reason=[{}], msg=[{}]", reason, msg);
        try {
            Buffer buffer = new Buffer(Message.DISCONNECT);
            buffer.putInt(reason.toInt());
            buffer.putString(msg);
            buffer.putString(""); // langtag..?
            writePacket(buffer);
        } catch (Exception e) {
            log.error("Error while disconnecting: {}", e.toString());
        } finally {
            stop();
        }
    }
    
    public Service getActiveService()
    {
        return service;
    }
    
    public String getClientVersion()
    {
        return clientID.substring(8);
    }
    
    public FactoryManager getFactoryManager()
    {
        return fm;
    }
    
    public byte[] getID()
    {
        return kex.sessionID;
    }
    
    public String getServerVersion()
    {
        return serverID == null ? serverID : serverID.substring(8);
    }
    
    void handle(Buffer packet) throws IOException
    {
        Message cmd = packet.getCommand();
        log.debug("Received packet {}", cmd);
        switch (cmd)
        {
        case DISCONNECT:
        {
            DisconnectReason code = DisconnectReason.fromInt(packet.getInt());
            String msg = packet.getString();
            log.info("Received SSH_MSG_DISCONNECT (reason={}, msg={})", code, msg);
            throw new SSHException(code, msg);
        }
        case UNIMPLEMENTED:
        {
            int code = packet.getInt();
            /*
             * If this packet ever becomes relevant, could notify interested observers.
             */
            log.info("Received SSH_MSG_UNIMPLEMENTED #{}", code);
            break;
        }
        case DEBUG:
        {
            boolean display = packet.getBoolean();
            String msg = packet.getString();
            log.info("Received SSH_MSG_DEBUG (display={}) '{}'", display, msg);
            break;
        }
        case IGNORE:
        {
            log.info("Received SSH_MSG_IGNORE");
            break;
        }
        default:
        {
            switch (state)
            {
            case KEX:
            {
                if (kex.handle(cmd, packet)) // key exchange completed
                    setState(State.KEX_DONE);
                break;
            }
            case SERVICE_REQ:
            {
                if (cmd != Message.SERVICE_ACCEPT) {
                    disconnect(DisconnectReason.PROTOCOL_ERROR,
                            "Protocol error: expected packet SSH_MSG_SERVICE_ACCEPT, got " + cmd);
                    return;
                }
                setState(State.SERVICE);
                break;
            }
            case SERVICE:
            {
                if (cmd != Message.KEXINIT)
                    service.handle(cmd, packet);
                else {
                    setState(State.KEX);
                    kex.init();
                    kex.handle(cmd, packet);
                }
                break;
            }
            case KEX_DONE:
                log.debug("Hmm? Unknown command received while in KEX_DONE");
                break;
            default:
                assert false;
            }
        }
        }
    }
    
    public void init(Socket socket) throws IOException
    {
        this.socket = socket;
        input = socket.getInputStream();
        output = socket.getOutputStream();
        
        log.info("Client identity string: {}", clientID);
        output.write((clientID + "\r\n").getBytes());
        
        Buffer buf = new Buffer();
        while ((serverID = readIdentification(buf)) == null)
            buf.putByte((byte) input.read());
        log.info("Server identity string: {}", serverID);
        
        setState(State.KEX);
        
        outPump.start();
        inPump.start();
        
        try {
            kex.init();
            waitFor(State.KEX_DONE);
        } catch (Exception e) {
            throw SSHException.chain(e);
        }
    }
    
    public boolean isRunning()
    {
        stateLock.lock();
        try {
            return !(state == State.ERROR || state == State.STOPPED);
        } finally {
            stateLock.unlock();
        }
    }
    
    private String readIdentification(Buffer buffer) throws IOException
    {
        String ident;
        
        byte[] data = new byte[256];
        for (;;) {
            int rpos = buffer.rpos();
            int pos = 0;
            boolean needLf = false;
            for (;;) {
                if (buffer.available() == 0) {
                    // need more data, so undo reading and return null
                    buffer.rpos(rpos);
                    return null;
                }
                byte b = buffer.getByte();
                if (b == '\r') {
                    needLf = true;
                    continue;
                }
                if (b == '\n')
                    break;
                if (needLf)
                    throw new SSHException("Incorrect identification: bad line ending");
                if (pos >= data.length)
                    throw new SSHException("Incorrect identification: line too long");
                data[pos++] = b;
            }
            ident = new String(data, 0, pos);
            if (ident.startsWith("SSH-"))
                break;
            if (buffer.rpos() > 16 * 1024)
                throw new SSHException("Incorrect identification: too many header lines");
        }
        
        if (!ident.startsWith("SSH-2.0-") && !ident.startsWith("SSH-1.99-"))
            throw new SSHException("Server does not support SSHv2, identified as: " + ident);
        
        return ident;
    }
    
    public synchronized void reqService(Service service) throws IOException
    {
        setState(State.SERVICE_REQ);
        sendServiceRequest(service.getName());
        try {
            waitFor(State.SERVICE);
            setService(service);
        } catch (Exception e) {
            throw SSHException.chain(e);
        }
    }
    
    private void sendServiceRequest(String serviceName) throws IOException
    {
        log.debug("Sending SSH_MSG_SERVICE_REQUEST for {}", serviceName);
        Buffer buffer = new Buffer(Message.SERVICE_REQUEST);
        buffer.putString(serviceName);
        writePacket(buffer);
    }
    
    /**
     * Send an unimplemented packet. This packet should contain the sequence id of the unsupported
     * packet.
     * 
     * @throws IOException
     *             if an error occured sending the packet
     */
    void sendUnimplemented(int num) throws IOException
    {
        Buffer buffer = new Buffer(Message.UNIMPLEMENTED);
        buffer.putInt(num);
        writePacket(buffer);
    }
    
    public void setAuthenticated()
    {
        authed = true;
    }
    
    /**
     * Specify how the client identifies itself
     * 
     * @param version
     */
    public void setClientVersion(String clientVersion)
    {
        clientID = "SSH-2.0-" + clientVersion;
    }
    
    /**
     * Used by inPump and outPump to notify of an exception, since it would otherwise escape
     * unnoticed.
     * 
     * @param e
     *            Exception
     */
    private synchronized void setError(Exception e)
    {
        // this method is idempotent except for state, but that should not affect anything
        
        log.error("Encountered error: {}", e.toString());
        
        if (service != null) {
            service.setError(SSHException.chain(e));
            service = null;
        }
        
        /*
         * will result in the exception being thrown in any thread that was waiting for state
         * change; see waitFor()
         */
        exception = e;
        setState(State.ERROR);
        
        stop();
    }
    
    public void setService(Service service)
    {
        log.info("Setting active service to {}", service.getName());
        this.service = service;
    }
    
    void setState(State newState)
    {
        log.debug("Changing state  [ {} -> {} ]", state, newState);
        stateLock.lock();
        try {
            state = newState;
            stateChange.signalAll();
        } finally {
            stateLock.unlock();
        }
    }
    
    void stop()
    {
        // stop inPump and outPump
        stopPumping = true;
        
        if (socket != null)
            try {
                socket.close(); // any threads blocked on it will die, yay
            } catch (IOException e) {
                log.debug("stop() ignoring {}", e);
            }
        // can safely reset these refs
        socket = null;
        input = null;
        output = null;
        
        // will wakeup any thread that was waiting for phase change, see waitFor()
        setState(State.STOPPED);
    }
    
    boolean verifyHost(PublicKey key)
    {
        HostKeyVerifier hkv;
        while ((hkv = hkvs.poll()) != null) {
            log.debug("Verifiying host key with [{}]", hkv);
            if (hkv.verify(socket.getInetAddress(), key))
                return true;
        }
        return false;
    }
    
    /**
     * Block for specified state.
     * 
     * @param s
     *            State
     * @throws Exception
     *             in case of error event while waiting
     */
    private void waitFor(State s) throws SSHException
    {
        stateLock.lock();
        try {
            while (state != s && state != State.ERROR && state != State.STOPPED)
                try {
                    stateChange.await();
                } catch (InterruptedException e) {
                    throw new SSHException(e);
                }
            log.debug("Woke up to {}", state.toString());
            if (state != s)
                if (state == State.ERROR)
                    throw SSHException.chain(exception);
                else if (state == State.STOPPED)
                    throw new SSHException(DisconnectReason.BY_APPLICATION);
        } finally {
            stateLock.unlock();
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.commons.net.ssh.transport.Session#writePacket(org.apache.commons.net.ssh.util.
     * Buffer)
     */
    public int writePacket(Buffer payload) throws IOException
    {
        /*
         * Synchronize all write requests as needed by the encoding algorithm and also queue the
         * write request here to ensure packets are sent in the correct order.
         * 
         * Besides while another thread that is writing a packet, writeLock may also be held while
         * key re-exchange is ongoing.
         */
        writeLock.lock();
        try {
            int seq = bin.encode(payload);
            byte[] data = payload.getCompactData();
            try {
                while (!outQ.offer(data, 1L, TimeUnit.SECONDS))
                    if (!outPump.isAlive())
                        throw new IOException("Output pumping thread is dead");
            } catch (InterruptedException e) {
                InterruptedIOException ioe = new InterruptedIOException();
                ioe.initCause(e);
                throw ioe;
            }
            return seq;
        } finally {
            writeLock.unlock();
        }
    }
    
}
