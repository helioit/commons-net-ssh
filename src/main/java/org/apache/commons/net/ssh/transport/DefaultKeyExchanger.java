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

import java.net.InetAddress;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.net.ssh.ErrorNotifiable;
import org.apache.commons.net.ssh.Factory;
import org.apache.commons.net.ssh.HostKeyVerifier;
import org.apache.commons.net.ssh.PacketHandler;
import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.cipher.Cipher;
import org.apache.commons.net.ssh.compression.Compression;
import org.apache.commons.net.ssh.digest.Digest;
import org.apache.commons.net.ssh.kex.KeyExchange;
import org.apache.commons.net.ssh.mac.MAC;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Event;
import org.apache.commons.net.ssh.util.SecurityUtils;
import org.apache.commons.net.ssh.util.Constants.DisconnectReason;
import org.apache.commons.net.ssh.util.Constants.KeyType;
import org.apache.commons.net.ssh.util.Constants.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Algorithm negotiation and key exchange
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class DefaultKeyExchanger implements PacketHandler, KeyExchanger
{
    
    private static enum Expected
    {
        /** we have sent or are sending KEXINIT, and expect the server's KEXINIT */
        KEXINIT,
        /** we are expecting some followup data as part of the exchange */
        FOLLOWUP,
        /** we are expecting SSH_MSG_NEWKEYS */
        NEWKEYS,
    }
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private Transport trans;
    private final AtomicBoolean kexOngoing = new AtomicBoolean();
    
    /** What we are expecting from the next packet */
    private Expected expected = Expected.KEXINIT;
    /** Negotiated algorithms */
    private String[] negotiated;
    /** Server's proposed algorithms - each string comma-delimited in order of preference */
    private String[] serverProposal;
    
    /**
     * {@link HostKeyVerifier#verify(InetAddress, PublicKey)} is invoked by
     * {@link #verifyHost(PublicKey)} when we are ready to verify the the server's host key.
     */
    private final Queue<HostKeyVerifier> hostVerifiers = new LinkedList<HostKeyVerifier>();
    
    /** Client's proposed algorithms - each string comma-delimited in order of preference */
    private String[] clientProposal;
    // Friendlier names for array indexes w.r.t the above 3 arrays
    private static final int PROP_KEX_ALG = 0;
    //private static final int PROP_SRVR_HOST_KEY_ALG = 1;
    private static final int PROP_ENC_ALG_C2S = 2;
    private static final int PROP_ENC_ALG_S2C = 3;
    private static final int PROP_MAC_ALG_C2S = 4;
    private static final int PROP_MAC_ALG_S2C = 5;
    private static final int PROP_COMP_ALG_C2S = 6;
    private static final int PROP_COMP_ALG_S2C = 7;
    private static final int PROP_LANG_C2S = 8;
    private static final int PROP_LANG_S2C = 9;
    
    private static final int PROP_MAX = 10;
    /** Instance of negotiated key exchange algorithm */
    private KeyExchange kex;
    /** Payload of our SSH_MSG_KEXINIT; is passed on to the KeyExchange alg */
    private byte[] I_C;
    
    /** Payload of server's SSH_MSG_KEXINIT; is passed on to the KeyExchange alg */
    private byte[] I_S;
    
    /** Computed session ID */
    private byte[] sessionID;
    
    private final ReentrantLock lock = new ReentrantLock();
    private final Event<TransportException> done = newEvent("kex done");
    
    private final Event<TransportException> kexInitSent = newEvent("kexinit sent");
    
    public synchronized void addHostKeyVerifier(HostKeyVerifier hkv)
    {
        hostVerifiers.add(hkv);
    }
    
    public byte[] getSessionID()
    {
        return sessionID;
    }
    
    public void handle(Message msg, Buffer buf) throws TransportException
    {
        switch (expected)
        {
        
        case KEXINIT:
            ensureReceivedMatchesExpected(msg, Message.KEXINIT);
            log.info("Received SSH_MSG_KEXINIT");
            startKex(false);
            kexInitSent.await(trans.getTimeout());
            gotKexInit(buf);
            expected = Expected.FOLLOWUP;
            break;
        
        case FOLLOWUP:
            ensureKexOngoing();
            log.info("Received kex followup data");
            buf.rpos(buf.rpos() - 1); // un-read the message byte
            if (kex.next(buf)) {
                verifyHost(kex.getHostKey());
                sendNewKeys();
                expected = Expected.NEWKEYS;
            }
            break;
        
        case NEWKEYS:
            ensureReceivedMatchesExpected(msg, Message.NEWKEYS);
            ensureKexOngoing();
            log.info("Received SSH_MSG_NEWKEYS");
            gotNewKeys();
            setKexDone();
            expected = Expected.KEXINIT;
            break;
        
        default:
            assert false;
            
        }
    }
    
    public void init(Transport trans)
    {
        this.trans = trans;
    }
    
    public boolean isKexDone()
    {
        return done.isSet();
    }
    
    public boolean isKexOngoing()
    {
        return kexOngoing.get();
    }
    
    public void notifyError(SSHException error)
    {
        log.debug("Got notified of {}", error);
        ErrorNotifiable.Util.alertAll(error, kexInitSent, done);
    }
    
    public void startKex(boolean waitForDone) throws TransportException
    {
        if (!kexOngoing.getAndSet(true)) {
            done.clear();
            sendKexInit();
        }
        if (waitForDone)
            waitForDone();
    }
    
    public void waitForDone() throws TransportException
    {
        done.await(trans.getTimeout());
    }
    
    private String[] createProposal()
    {
        return new String[] { //
        Factory.Util.getNames(trans.getConfig().getKeyExchangeFactories()), // PROP_KEX_ALG 
                Factory.Util.getNames(trans.getConfig().getSignatureFactories()), // PROP_SRVR_HOST_KEY_ALG
                Factory.Util.getNames(trans.getConfig().getCipherFactories()), // PROP_ENC_ALG_C2S
                Factory.Util.getNames(trans.getConfig().getCipherFactories()), // PROP_ENC_ALG_S2C
                Factory.Util.getNames(trans.getConfig().getMACFactories()), // PROP_MAC_ALG_C2S
                Factory.Util.getNames(trans.getConfig().getMACFactories()), // PROP_MAC_ALG_S2C
                Factory.Util.getNames(trans.getConfig().getCompressionFactories()), // PROP_MAC_ALG_C2S
                Factory.Util.getNames(trans.getConfig().getCompressionFactories()), // PROP_COMP_ALG_S2C
                "", // PROP_LANG_C2S (optional, thus empty string) 
                "" // PROP_LANG_S2C (optional, thus empty string) 
        };
    }
    
    private synchronized void ensureKexOngoing() throws TransportException
    {
        if (!isKexOngoing())
            throw new TransportException(DisconnectReason.PROTOCOL_ERROR,
                                         "Key exchange packet received when key exchange was not ongoing");
    }
    
    private void ensureReceivedMatchesExpected(Message got, Message expected) throws TransportException
    {
        if (got != expected)
            throw new TransportException(DisconnectReason.PROTOCOL_ERROR, "Was expecting " + expected);
    }
    
    private void extractProposal(Buffer buffer)
    {
        serverProposal = new String[PROP_MAX];
        // recreate the packet payload which will be needed at a later time
        byte[] d = buffer.array();
        I_S = new byte[buffer.available() + 1];
        I_S[0] = Message.KEXINIT.toByte();
        System.arraycopy(d, buffer.rpos(), I_S, 1, I_S.length - 1);
        // skip 16 bytes of random data
        buffer.rpos(buffer.rpos() + 16);
        // read proposal
        for (int i = 0; i < serverProposal.length; i++)
            serverProposal[i] = buffer.getString();
    }
    
    private void gotKexInit(Buffer buf) throws TransportException
    {
        extractProposal(buf);
        negotiate();
        kex = Factory.Util.create(trans.getConfig().getKeyExchangeFactories(), negotiated[PROP_KEX_ALG]);
        kex.init(trans, trans.getServerID().getBytes(), trans.getClientID().getBytes(), I_S, I_C);
    }
    
    /**
     * Put new keys into use. This method will intialize the ciphers, digests, MACs and compression
     * according to the negotiated server and client proposals.
     */
    private void gotNewKeys()
    {
        byte[] IVc2s;
        byte[] IVs2c;
        byte[] Ec2s;
        byte[] Es2c;
        byte[] MACc2s;
        byte[] MACs2c;
        byte[] K = kex.getK();
        byte[] H = kex.getH();
        Digest hash = kex.getHash();
        Cipher s2ccipher;
        Cipher c2scipher;
        MAC s2cmac;
        MAC c2smac;
        Compression s2ccomp;
        Compression c2scomp;
        
        if (sessionID == null) {
            sessionID = new byte[H.length];
            System.arraycopy(H, 0, sessionID, 0, H.length);
        }
        
        Buffer buffer = new Buffer().putMPInt(K) //
                                    .putRawBytes(H) //
                                    .putByte((byte) 0x41) //
                                    .putRawBytes(sessionID);
        int pos = buffer.available();
        byte[] buf = buffer.array();
        hash.update(buf, 0, pos);
        IVc2s = hash.digest();
        
        int j = pos - sessionID.length - 1;
        
        buf[j]++;
        hash.update(buf, 0, pos);
        IVs2c = hash.digest();
        
        buf[j]++;
        hash.update(buf, 0, pos);
        Ec2s = hash.digest();
        
        buf[j]++;
        hash.update(buf, 0, pos);
        Es2c = hash.digest();
        
        buf[j]++;
        hash.update(buf, 0, pos);
        MACc2s = hash.digest();
        
        buf[j]++;
        hash.update(buf, 0, pos);
        MACs2c = hash.digest();
        
        s2ccipher = Factory.Util.create(trans.getConfig().getCipherFactories(), negotiated[PROP_ENC_ALG_S2C]);
        Es2c = resizeKey(Es2c, s2ccipher.getBlockSize(), hash, K, H);
        s2ccipher.init(Cipher.Mode.Decrypt, Es2c, IVs2c);
        
        s2cmac = Factory.Util.create(trans.getConfig().getMACFactories(), negotiated[PROP_MAC_ALG_S2C]);
        s2cmac.init(MACs2c);
        
        c2scipher = Factory.Util.create(trans.getConfig().getCipherFactories(), negotiated[PROP_ENC_ALG_C2S]);
        Ec2s = resizeKey(Ec2s, c2scipher.getBlockSize(), hash, K, H);
        c2scipher.init(Cipher.Mode.Encrypt, Ec2s, IVc2s);
        
        c2smac = Factory.Util.create(trans.getConfig().getMACFactories(), negotiated[PROP_MAC_ALG_C2S]);
        c2smac.init(MACc2s);
        
        s2ccomp = Factory.Util.create(trans.getConfig().getCompressionFactories(), negotiated[PROP_COMP_ALG_S2C]);
        c2scomp = Factory.Util.create(trans.getConfig().getCompressionFactories(), negotiated[PROP_COMP_ALG_C2S]);
        
        trans.setClientToServerAlgorithms(c2scipher, c2smac, c2scomp);
        trans.setServerToClientAlgorithms(s2ccipher, s2cmac, s2ccomp);
    }
    
    /**
     * Compute the negotiated proposals by merging the client and server proposal. The negotiated
     * proposal will be stored in the {@link #negotiated} field.
     */
    private void negotiate() throws TransportException
    {
        String[] guess = new String[PROP_MAX];
        for (int i = 0; i < PROP_MAX; i++) {
            String[] c = clientProposal[i].split(",");
            String[] s = serverProposal[i].split(",");
            for (String ci : c) {
                for (String si : s)
                    if (ci.equals(si)) { // first match wins
                        guess[i] = ci;
                        break;
                    }
                if (guess[i] != null)
                    break;
            }
            if (guess[i] == null && // 
                    i != PROP_LANG_C2S && i != PROP_LANG_S2C) // since we don't negotiate languages
                throw new TransportException("Unable to negotiate");
        }
        negotiated = guess;
        
        log.info("Negotiated algorithms: client -> server = (" + negotiated[PROP_ENC_ALG_C2S] + ", "
                + negotiated[PROP_MAC_ALG_C2S] + ", " + negotiated[PROP_COMP_ALG_C2S] + ") | server -> client = ("
                + negotiated[PROP_ENC_ALG_S2C] + ", " + negotiated[PROP_MAC_ALG_S2C] + ", "
                + negotiated[PROP_COMP_ALG_S2C] + ")");
    }
    
    private Event<TransportException> newEvent(String name)
    {
        return new Event<TransportException>(name, TransportException.chainer, lock);
    }
    
    /**
     * Private method used while putting new keys into use that will resize the key used to
     * initialize the cipher to the needed length.
     * 
     * @param E
     *            the key to resize
     * @param blockSize
     *            the cipher block size
     * @param hash
     *            the hash algorithm
     * @param K
     *            the key exchange K parameter
     * @param H
     *            the key exchange H parameter
     * @return the resized key
     */
    private byte[] resizeKey(byte[] E, int blockSize, Digest hash, byte[] K, byte[] H)
    {
        while (blockSize > E.length) {
            Buffer buffer = new Buffer().putMPInt(K) //
                                        .putRawBytes(H) //
                                        .putRawBytes(E);
            hash.update(buffer.array(), 0, buffer.available());
            byte[] foo = hash.digest();
            byte[] bar = new byte[E.length + foo.length];
            System.arraycopy(E, 0, bar, 0, E.length);
            System.arraycopy(foo, 0, bar, E.length, foo.length);
            E = bar;
        }
        return E;
    }
    
    private void sendKexInit() throws TransportException
    {
        Buffer buf = new Buffer(Message.KEXINIT);
        
        // Put cookie
        int p = buf.wpos();
        buf.wpos(p + 16);
        trans.getPRNG().fill(buf.array(), p, 16);
        
        // Put the 10 name-list's
        for (String s : clientProposal = createProposal())
            buf.putString(s);
        
        buf.putBoolean(false) // Optimistic next packet does not follow
           .putInt(0); // "Reserved" for future by spec
        
        I_C = buf.getCompactData(); // Store for future
        
        log.info("Sending SSH_MSG_KEXINIT");
        trans.writePacket(buf);
        
        kexInitSent.set();
    }
    
    private void sendNewKeys() throws TransportException
    {
        log.info("Sending SSH_MSG_NEWKEYS");
        trans.writePacket(new Buffer(Message.NEWKEYS));
    }
    
    private void setKexDone()
    {
        kexOngoing.set(false);
        kexInitSent.clear();
        done.set();
    }
    
    /**
     * Tries to validate host key with all the host key verifiers known to this instance (
     * {@link #hostVerifiers})
     * 
     * @param key
     *            the host key to verify
     */
    private synchronized void verifyHost(PublicKey key) throws TransportException
    {
        for (HostKeyVerifier hkv : hostVerifiers) {
            log.debug("Trying to verify host key with {}", hkv);
            if (hkv.verify(trans.getRemoteHost(), key))
                return;
        }
        
        throw new TransportException(DisconnectReason.HOST_KEY_NOT_VERIFIABLE, "Could not verify ["
                + KeyType.fromKey(key) + "] host key with fingerprint [" + SecurityUtils.getFingerprint(key) + "]");
    }
    
}