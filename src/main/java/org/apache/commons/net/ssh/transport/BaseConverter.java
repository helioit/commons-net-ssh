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

/**
 * Encoding / decoding of packets in the SSH binary protocol
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
class BaseConverter implements Converter
{
    
    protected Cipher cipher;
    protected int cipherSize = 8;
    protected MAC mac;
    protected Compression compression;
    protected long seq = -1;
    protected boolean authed;
    
    public long getSequenceNumber()
    {
        return seq;
    }
    
    public synchronized void setAlgorithms(Cipher cipher, MAC mac, Compression compression)
    {
        this.cipher = cipher;
        this.mac = mac;
        this.compression = compression;
        this.cipherSize = cipher.getIVSize();
    }
    
    public synchronized void setAuthenticated()
    {
        this.authed = true;
    }
    
}
