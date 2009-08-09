package org.apache.commons.net.ssh.transport;

import org.apache.commons.net.ssh.cipher.Cipher;
import org.apache.commons.net.ssh.compression.Compression;
import org.apache.commons.net.ssh.mac.MAC;

interface Converter
{
    
    long getSequenceNumber();
    
    void setAlgorithms(Cipher cipher, MAC mac, Compression compression);
    
    void setAuthenticated();
    
}