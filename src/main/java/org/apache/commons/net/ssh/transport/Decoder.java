package org.apache.commons.net.ssh.transport;

import org.apache.commons.net.ssh.PacketHandler;
import org.apache.commons.net.ssh.SSHException;

public interface Decoder extends Converter
{
    
    int getMaxPacketLength();
    
    void init(PacketHandler packetHandler);
    
    int received(byte[] b, int len) throws SSHException;
    
}