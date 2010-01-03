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

import org.apache.commons.net.ssh.cipher.Cipher;
import org.apache.commons.net.ssh.cipher.NoneCipher;
import org.apache.commons.net.ssh.compression.Compression;
import org.apache.commons.net.ssh.mac.MAC;

/**
 * Base class for {@link Encoder} and {@link Decoder}.
 * <p>
 * From RFC 4253, p. 6
 * 
 * <pre>
 *    Each packet is in the following format:
 * 
 *       uint32    packet_length
 *       byte      padding_length
 *       byte[n1]  payload; n1 = packet_length - padding_length - 1
 *       byte[n2]  random padding; n2 = padding_length
 *       byte[m]   mac (Message Authentication Code - MAC); m = mac_length
 * </pre>
 */
class Converter
{
    
    protected Cipher cipher = new NoneCipher();
    protected MAC mac = null;
    protected Compression compression = null;
    
    protected int cipherSize = 8;
    protected long seq = -1;
    protected boolean authed;
    
    long getSequenceNumber()
    {
        return seq;
    }
    
    void setAlgorithms(Cipher cipher, MAC mac, Compression compression)
    {
        this.cipher = cipher;
        this.mac = mac;
        this.compression = compression;
        this.cipherSize = cipher.getIVSize();
    }
    
    void setAuthenticated()
    {
        this.authed = true;
    }
    
}