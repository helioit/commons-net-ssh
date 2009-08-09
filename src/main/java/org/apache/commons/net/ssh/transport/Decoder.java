package org.apache.commons.net.ssh.transport;

import org.apache.commons.net.ssh.PacketHandler;
import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.util.Buffer;

public interface Decoder extends Converter
{
    
    int getMaxPacketLength();
    
    /**
     * Call this method for every byte received.
     * <p>
     * When enough data has been received to decode a complete packet,
     * {@link TransportProtocol#handle(Buffer)} will be called.
     */
    int received(byte[] b, int len) throws SSHException;
    
    void init(PacketHandler packetHandler);
    
}