package org.apache.commons.net.ssh.transport;

import org.apache.commons.net.ssh.cipher.Cipher;
import org.apache.commons.net.ssh.compression.Compression;
import org.apache.commons.net.ssh.mac.MAC;
import org.apache.commons.net.ssh.prng.PRNG;
import org.apache.commons.net.ssh.util.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A thread encoding and sending packets is required to hold this object's monitor while doing so.
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class DefaultEncoder extends BaseConverter implements Encoder
{
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected PRNG prng;
    
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
    
    public void init(PRNG prng)
    {
        this.prng = prng;
    }
    
    @Override
    public synchronized void setAlgorithms(Cipher cipher, MAC mac, Compression compression)
    {
        super.setAlgorithms(cipher, mac, compression);
        if (compression != null)
            compression.init(Compression.Type.Deflater, -1);
    }
    
}
