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
     * Encode a buffer into the SSH binary protocol per the current algorithms
     * 
     * @param buffer
     *            the buffer to encode
     * @return the sequence no. of encoded packet
     * @throws TransportException
     */
    public long encode(Buffer buffer) throws TransportException
    {
        // Check that the packet has some free space for the header
        if (buffer.rpos() < 5) {
            log.warn("Performance cost: when sending a packet, ensure that "
                    + "5 bytes are available in front of the buffer");
            Buffer nb = new Buffer(buffer.available() + 5);
            nb.rpos(5);
            nb.wpos(5);
            nb.putBuffer(buffer);
            buffer = nb;
        }
        
        // Debug log the packet
        if (log.isTraceEnabled())
            log.trace("Sending packet #{}: {}", seq, buffer.printHex());
        
        // Compress the packet if needed
        if (compression != null && (authed || !compression.isDelayed()))
            compression.compress(buffer);
        
        // Grab the length of the packet (excluding the 5 header bytes)
        int len = buffer.available();
        int off = buffer.rpos() - 5;
        
        // Compute padding length
        int bsize = cipherSize;
        int oldLen = len;
        len += 5;
        int pad = -len & bsize - 1;
        if (pad < bsize)
            pad += bsize;
        len = len + pad - 4;
        
        // Write 5 header bytes
        buffer.wpos(off);
        buffer.putInt(len);
        buffer.putByte((byte) pad);
        
        // Fill padding
        buffer.wpos(off + oldLen + 5 + pad);
        prng.fill(buffer.array(), buffer.wpos() - pad, pad);
        
        seq = seq + 1 & 0xffffffffL;
        // Compute MAC
        if (mac != null) {
            int macSize = mac.getBlockSize();
            int l = buffer.wpos();
            buffer.wpos(l + macSize);
            mac.update(seq);
            mac.update(buffer.array(), off, l);
            mac.doFinal(buffer.array(), l);
        }
        
        // Encrypt packet, excluding mac
        if (cipher != null)
            cipher.update(buffer.array(), off, len + 4);
        
        buffer.rpos(off); // Make buffer ready to be read
        
        return seq;
    }
    
    @Override
    public synchronized void setAlgorithms(Cipher cipher, MAC mac, Compression compression)
    {
        super.setAlgorithms(cipher, mac, compression);
        if (compression != null)
            compression.init(Compression.Type.Deflater, -1);
    }
    
}