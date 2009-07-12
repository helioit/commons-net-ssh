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
package org.apache.commons.net.ssh;

import java.net.InetAddress;
import java.security.PublicKey;

import org.apache.commons.net.ssh.util.SecurityUtils;

/**
 * Interface for host key verification.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public interface HostKeyVerifier
{
    
    class Util
    {
        
        public static HostKeyVerifier makeForFingerprint(final String fp)
        {
            return new HostKeyVerifier()
                {
                    
                    public boolean verify(InetAddress address, PublicKey key)
                    {
                        return SecurityUtils.getFingerprint(key).equals(fp);
                    }
                    
                };
        }
        
    }
    
    /**
     * This callback is invoked when the server's host key needs to be verified. The return value
     * indicates to the caller whether the SSH connection should proceed.
     * <p>
     * <b>Note</b>: host key verification is the basis for security in SSH, therefore exercise due
     * caution in implementing!
     * 
     * @param address
     *            remote address we are connected to
     * @param key
     *            host key of server
     * @return <code>true</code> if key is acceptable, <code>false</code> otherwise
     */
    boolean verify(InetAddress address, PublicKey key);
    
}
