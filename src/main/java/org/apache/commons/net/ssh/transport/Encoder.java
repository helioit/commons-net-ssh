package org.apache.commons.net.ssh.transport;

import org.apache.commons.net.ssh.cipher.Cipher;
import org.apache.commons.net.ssh.compression.Compression;
import org.apache.commons.net.ssh.mac.MAC;
import org.apache.commons.net.ssh.random.Random;
import org.apache.commons.net.ssh.util.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class Encoder extends Converter
{
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final Random prng;
    
    Encoder(Random prng)
    {
        this.prng = prng;
    }
    
    /**
     * Encode a buffer into the SSH binary protocol as per the negotiated algorithms.
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
            Buffer nb = new Buffer();
            nb.wpos(5);
            nb.putBuffer(buffer);
            buffer = nb;
        }
        
        // Grab the length of the packet (excluding the 5 header bytes)
        int len = buffer.available();
        int off = buffer.rpos() - 5;
        
        // Debug log the packet
        if (log.isTraceEnabled())
            log.trace("Sending packet #{}: {}", seq, buffer.printHex());
        
        // Compress the packet if needed
        if (compression != null && (authed || !compression.isDelayed())) {
            compression.compress(buffer);
            len = buffer.available();
        }
        
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
        
        long seqNumforCurrentPacket = seq;
        seq = seq + 1 & 0xffffffffL; // applicable to next packet 
        return seqNumforCurrentPacket;
    }
    
    @Override
    public void setAlgorithms(Cipher cipher, MAC mac, Compression compression)
    {
        super.setAlgorithms(cipher, mac, compression);
        if (compression != null)
            compression.init(Compression.Type.Deflater, -1);
    }
    
}
