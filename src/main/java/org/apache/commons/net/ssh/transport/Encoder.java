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
import org.apache.commons.net.ssh.compression.Compression;
import org.apache.commons.net.ssh.mac.MAC;
import org.apache.commons.net.ssh.random.Random;
import org.apache.commons.net.ssh.util.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Encoder extends Converter
{
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final Random prng;
    
    Encoder(Random prng)
    {
        this.prng = prng;
    }
    
    /**
     * Encode a buffer into the SSH binary protocol per the current algorithms.
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
     * 
     * @param buffer
     *            the buffer to encode
     * @return the sequence no. of encoded packet
     * @throws TransportException
     */
    public long encode(Buffer buffer) throws TransportException
    {
        buffer = checkHeaderSpace(buffer);
        
        if (log.isTraceEnabled())
            log.trace("Sending packet #{}: {}", seq, buffer.printHex());
        
        compress(buffer);
        
        final int payloadSize = buffer.available();
        
        // Compute padding length
        int padLen = -(payloadSize + 5) & cipherSize - 1;
        if (padLen < cipherSize)
            padLen += cipherSize;
        
        final int startOfPacket = buffer.rpos() - 5;
        final int packetLen = payloadSize + 1 + padLen;
        
        // Put packet header
        buffer.wpos(startOfPacket);
        buffer.putInt(packetLen);
        buffer.putByte((byte) padLen);
        
        // Now wpos will mark end of padding
        buffer.wpos(startOfPacket + 5 + payloadSize + padLen);
        // Fill padding
        prng.fill(buffer.array(), buffer.wpos() - padLen, padLen);
        
        seq = seq + 1 & 0xffffffffL;
        
        putMAC(buffer, startOfPacket, buffer.wpos());
        
        cipher.update(buffer.array(), startOfPacket, 4 + packetLen);
        
        buffer.rpos(startOfPacket); // Make ready-to-read
        
        return seq;
    }
    
    @Override
    public synchronized void setAlgorithms(Cipher cipher, MAC mac, Compression compression)
    {
        super.setAlgorithms(cipher, mac, compression);
        if (compression != null)
            compression.init(Compression.Type.Deflater, -1);
    }
    
    private Buffer checkHeaderSpace(Buffer buffer)
    {
        if (buffer.rpos() < 5) {
            log.warn("Performance cost: when sending a packet, ensure that "
                    + "5 bytes are available in front of the buffer");
            Buffer nb = new Buffer(buffer.available() + 5);
            nb.rpos(5);
            nb.wpos(5);
            nb.putBuffer(buffer);
            buffer = nb;
        }
        return buffer;
    }
    
    private void compress(Buffer buffer) throws TransportException
    {
        // Compress the packet if needed
        if (compression != null && (authed || !compression.isDelayed()))
            compression.compress(buffer);
    }
    
    private void putMAC(Buffer buffer, int startOfPacket, int endOfPadding)
    {
        if (mac != null) {
            mac.update(seq);
            mac.update(buffer.array(), startOfPacket, endOfPadding);
            mac.doFinal(buffer.array(), endOfPadding);
            buffer.wpos(endOfPadding + mac.getBlockSize());
        }
    }
    
}