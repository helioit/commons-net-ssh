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

import org.apache.commons.net.ssh.digest.Digest;
import org.apache.commons.net.ssh.transport.Transport;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;

/**
 * Key exchange algorithm.
 */
public interface KeyExchange
{
    
    /**
     * Retrieves the computed H parameter
     * 
     * @return
     */
    byte[] getH();
    
    /**
     * The message digest used by this key exchange algorithm.
     * 
     * @return the message digest
     */
    Digest getHash();
    
    PublicKey getHostKey();
    
    /**
     * Retrieves the computed K parameter
     * 
     * @return
     */
    byte[] getK();
    
    /**
     * Initialize the key exchange algorithm.
     * 
     * @param trans
     *            the transport layer that is using this alg.
     * @param V_S
     *            the server identification string
     * @param V_C
     *            the client identification string
     * @param I_S
     *            the server key init packet
     * @param I_C
     *            the client key init packet
     * @throws TransportException
     *             if an error occurs
     */
    void init(Transport trans, byte[] V_S, byte[] V_C, byte[] I_S, byte[] I_C) throws TransportException;
    
    /**
     * Process the next packet
     * 
     * @param buffer
     *            the packet
     * @return a boolean indicating if the processing is complete or if more packets are to be received
     * @throws TransportException
     *             if an error occurs
     */
    boolean next(Buffer buffer) throws TransportException;
    
}
