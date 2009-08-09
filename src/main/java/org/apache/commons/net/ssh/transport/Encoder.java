package org.apache.commons.net.ssh.transport;

import org.apache.commons.net.ssh.prng.PRNG;
import org.apache.commons.net.ssh.util.Buffer;

public interface Encoder extends Converter
{
    
    /**
     * Encode a buffer into the SSH binary protocol as per the negotiated algorithms.
     * 
     * @param buffer
     *            the buffer to encode
     * @return the sequence no. of encoded packet
     * @throws TransportException
     */
    long encode(Buffer buffer) throws TransportException;
    
    void init(PRNG prng);
    
}