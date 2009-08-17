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
package org.apache.commons.net.ssh.userauth;

import java.io.IOException;

import org.apache.commons.net.ssh.keyprovider.KeyProvider;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Constants.Message;

/**
 * Implements the {@code "publickey"} SSH authentication method.
 * <p>
 * Requesteing authentication with this method first sends a "feeler" request with just the public
 * key, and if the server responds with {@code SSH_MSG_USERAUTH_PK_OK} indicating that the key is
 * acceptable, it proceeds to send a request signed with the private key. Therefore, private keys
 * are not requested from the associated {@link KeyProvider} unless needed.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class AuthPublickey extends KeyedAuthMethod
{
    
    /**
     * Initialize this method with the provider for public and private key.
     */
    public AuthPublickey(KeyProvider kProv)
    {
        super("publickey", kProv);
    }
    
    /**
     * Internal use.
     */
    @Override
    public void handle(Message cmd, Buffer buf) throws UserAuthException, TransportException
    {
        if (cmd == Message.USERAUTH_60)
            sendSignedReq();
        else
            super.handle(cmd, buf);
    }
    
    /**
     * Builds SSH_MSG_USERAUTH_REQUEST packet.
     * 
     * @param signed
     *            whether the request packet will contain signature
     * @return the {@link Buffer} containing the request packet
     * @throws UserAuthException
     */
    private Buffer buildReq(boolean signed) throws UserAuthException
    {
        try {
            kProv.getPublic();
        } catch (IOException ioe) {
            throw new UserAuthException("Problem getting public key", ioe);
        }
        return putPubKey(super.buildReq().putBoolean(signed));
    }
    
    /**
     * Send SSH_MSG_USERAUTH_REQUEST containing the signature.
     * 
     * @throws UserAuthException
     * @throws TransportException
     */
    private void sendSignedReq() throws UserAuthException, TransportException
    {
        log.debug("Sending signed request");
        params.getTransport().writePacket(putSig(buildReq(true)));
    }
    
    /**
     * Builds a feeler request (sans signature).
     */
    @Override
    protected Buffer buildReq() throws UserAuthException
    {
        return buildReq(false);
    }
    
}
