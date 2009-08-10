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
package org.apache.commons.net.ssh.kex;

import java.security.PublicKey;

import org.apache.commons.net.ssh.Factory;
import org.apache.commons.net.ssh.digest.Digest;
import org.apache.commons.net.ssh.digest.SHA1;
import org.apache.commons.net.ssh.signature.Signature;
import org.apache.commons.net.ssh.transport.Transport;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Constants.DisconnectReason;
import org.apache.commons.net.ssh.util.Constants.KeyType;
import org.apache.commons.net.ssh.util.Constants.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for DHG key exchange algorithms. Implementations will only have to configure the
 * required data on the {@link DH} class in the {@link #initDH(org.apache.sshd.common.kex.DH)}
 * method.
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public abstract class AbstractDHG implements KeyExchange
{
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private Transport trans;
    private byte[] V_S;
    private byte[] V_C;
    private byte[] I_S;
    private byte[] I_C;
    private Digest sha;
    private DH dh;
    private byte[] e;
    private byte[] f;
    private byte[] K;
    private byte[] H;
    private PublicKey hostKey;
    
    public byte[] getH()
    {
        return H;
    }
    
    public Digest getHash()
    {
        return sha;
    }
    
    public PublicKey getHostKey()
    {
        return hostKey;
    }
    
    public byte[] getK()
    {
        return K;
    }
    
    public void init(Transport trans, byte[] V_S, byte[] V_C, byte[] I_S, byte[] I_C) throws TransportException
    {
        this.trans = trans;
        this.V_S = V_S;
        this.V_C = V_C;
        this.I_S = I_S;
        this.I_C = I_C;
        sha = new SHA1();
        sha.init();
        dh = new DH();
        initDH(dh);
        e = dh.getE();
        
        log.info("Sending SSH_MSG_KEXDH_INIT");
        trans.writePacket(new Buffer(Message.KEXDH_INIT).putMPInt(e));
    }
    
    public boolean next(Buffer buffer) throws TransportException
    {
        Message msg = buffer.getMessageID();
        if (msg != Message.KEXDH_31)
            throw new TransportException(DisconnectReason.KEY_EXCHANGE_FAILED, "Unxpected packet: " + msg);
        
        log.info("Received SSH_MSG_KEXDH_REPLY");
        byte[] K_S = buffer.getBytes();
        f = buffer.getMPIntAsBytes();
        byte[] sig = buffer.getBytes(); // signature sent by server
        dh.setF(f);
        K = dh.getK();
        
        hostKey = new Buffer(K_S).getPublicKey();
        
        buffer = new Buffer() // our hash
                             .putString(V_C) // 
                             .putString(V_S) // 
                             .putString(I_C) //
                             .putString(I_S) //
                             .putString(K_S) //
                             .putMPInt(e) //
                             .putMPInt(f) //
                             .putMPInt(K); //
        sha.update(buffer.array(), 0, buffer.available());
        H = sha.digest();
        
        Signature verif = Factory.Util.create(trans.getConfig().getSignatureFactories(), // 
                                              KeyType.fromKey(hostKey).toString());
        verif.init(hostKey, null);
        verif.update(H, 0, H.length);
        if (!verif.verify(sig))
            throw new TransportException(DisconnectReason.KEY_EXCHANGE_FAILED,
                                         "KeyExchange signature verification failed");
        return true;
    }
    
    protected abstract void initDH(DH dh);
    
}
