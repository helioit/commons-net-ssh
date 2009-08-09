package org.apache.commons.net.ssh.transport;

import org.apache.commons.net.ssh.PacketHandler;
import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.cipher.Cipher;
import org.apache.commons.net.ssh.compression.Compression;
import org.apache.commons.net.ssh.mac.MAC;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.BufferUtils;
import org.apache.commons.net.ssh.util.Constants.DisconnectReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Not thread-safe; single producer assumed.
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class Decoder extends Converter
{
    
    public static final int MAX_PACKET_LEN = 256 * 1024;
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    /** What we pass decoded packets to */
    protected final PacketHandler packetHandler;
    /** Buffer where as-yet undecoded data lives */
    protected final Buffer inputBuffer = new Buffer();
    protected final Buffer uncompressBuffer = new Buffer();
    /** MAC result is stored here */
    protected byte[] macResult;
    /** -1 if packet length not yet been decoded, else the packet length */
    protected int packetLength = -1;
    
    /**
     * How many bytes do we need, before a call to decode() can succeed at decoding at least packet
     * length, OR the whole packet?
     */
    private int needed = 8;
    
    Decoder(PacketHandler packetHandler)
    {
        this.packetHandler = packetHandler;
    }
    
    /**
     * Call this method for every byte received.
     * <p>
     * When enough data has been received to decode a complete packet,
     * {@link TransportProtocol#handle(Buffer)} will be called.
     */
    public int received(byte[] b, int len) throws SSHException
    {
        inputBuffer.putRawBytes(b, 0, len);
        if (needed <= len)
            needed = decode();
        else
            needed -= len;
        return needed;
    }
    
    @Override
    public void setAlgorithms(Cipher cipher, MAC mac, Compression compression)
    {
        super.setAlgorithms(cipher, mac, compression);
        macResult = new byte[mac.getBlockSize()];
        if (compression != null)
            compression.init(Compression.Type.Inflater, -1);
    }
    
    /**
     * Decodes incoming buffer; when a packet has been decoded hooks in to
     * {@link PacketHandler#handle}.
     * <p>
     * Returns advised number of bytes that should be made available in decoderBuffer before the
     * method should be called again.
     * 
     * @return number of bytes needed before further decoding possible
     */
    protected int decode() throws SSHException
    {
        int need;
        
        // Decoding loop
        for (;;)
            
            if (packetLength == -1) // Waiting for beginning of packet
            {
                
                // The read position should always be 0 at this point because we have compacted this
                // buffer
                assert inputBuffer.rpos() == 0;
                // If we have received enough bytes, start processing those
                need = cipherSize - inputBuffer.available();
                if (need <= 0) {
                    // Decrypt the first bytes
                    if (cipher != null)
                        cipher.update(inputBuffer.array(), 0, cipherSize);
                    // Read packet length
                    packetLength = inputBuffer.getInt();
                    // Check packet length validity
                    if (packetLength < 5 || packetLength > MAX_PACKET_LEN) {
                        log.info("Error decoding packet (invalid length) {}", inputBuffer.printHex());
                        throw new TransportException(DisconnectReason.PROTOCOL_ERROR, "invalid packet length: "
                                + packetLength);
                    }
                } else
                    break;
                
            } else {
                
                // The read position should always be 4 at this point
                assert inputBuffer.rpos() == 4;
                
                int macSize = mac != null ? mac.getBlockSize() : 0;
                
                // Check if the packet has been fully received
                need = packetLength + macSize - inputBuffer.available();
                if (need <= 0) {
                    
                    byte[] data = inputBuffer.array();
                    
                    // Decrypt the rest of the packet
                    if (cipher != null)
                        cipher.update(data, cipherSize, packetLength + 4 - cipherSize);
                    
                    if (mac != null) {
                        // Update MAC with packet id
                        mac.update(seq);
                        // Update MAC with packet data
                        mac.update(data, 0, packetLength + 4);
                        // Compute MAC result
                        mac.doFinal(macResult, 0);
                        // Check the computed result with the received mac (just
                        // after the packet data)
                        if (!BufferUtils.equals(macResult, 0, data, packetLength + 4, macSize))
                            throw new TransportException(DisconnectReason.MAC_ERROR, "MAC Error");
                    }
                    
                    int wpos = inputBuffer.wpos();
                    
                    // Get padding
                    byte pad = inputBuffer.getByte();
                    
                    Buffer decoded;
                    // Decompress if needed
                    if (compression != null && (authed || !compression.isDelayed())) {
                        uncompressBuffer.clear();
                        inputBuffer.wpos(inputBuffer.rpos() + packetLength - 1 - pad);
                        compression.uncompress(inputBuffer, uncompressBuffer);
                        decoded = uncompressBuffer;
                    } else {
                        inputBuffer.wpos(packetLength + 4 - pad);
                        decoded = inputBuffer;
                    }
                    
                    if (log.isTraceEnabled())
                        log.trace("Received packet #{}: {}", seq, decoded.printHex());
                    
                    // ------------------------------------------------- //
                    packetHandler.handle(decoded.getMessageID(), decoded); // process the decoded packet //
                    // ------------------------------------------------- //
                    
                    // Set ready to handle next packet
                    inputBuffer.rpos(packetLength + 4 + macSize);
                    inputBuffer.wpos(wpos);
                    inputBuffer.compact();
                    packetLength = -1;
                    
                    // Increment incoming packet sequence number (i.e. applicable to next packet)
                    seq = seq + 1 & 0xffffffffL;
                    
                } else
                    // need more data
                    break;
                
            }
        
        return need;
    }
    
}
