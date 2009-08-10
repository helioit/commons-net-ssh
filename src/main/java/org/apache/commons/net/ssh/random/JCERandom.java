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
package org.apache.commons.net.ssh.random;

import java.security.SecureRandom;

/**
 * A {@link Random} implementation using the built-in {@link SecureRandom} PRNG.
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public class JCERandom implements Random
{
    
    /**
     * Named factory for the JCE {@link Random}
     */
    public static class Factory implements org.apache.commons.net.ssh.Factory.Named<Random>
    {
        
        public Random create()
        {
            return new JCERandom();
        }
        
        public String getName()
        {
            return "default";
        }
        
    }
    
    private byte[] tmp = new byte[16];
    private SecureRandom random = null;
    
    public JCERandom()
    {
        random = new SecureRandom();
    }
    
    /**
     * Fill the given byte-array with random bytes from this PRNG.
     * 
     * @param foo
     *            the byte-array
     * @param start
     *            the offset to start at
     * @param len
     *            the number of bytes to fill
     */
    public synchronized void fill(byte[] foo, int start, int len)
    {
        if (start == 0 && len == foo.length)
            random.nextBytes(foo);
        else
            synchronized (this) {
                if (len > tmp.length)
                    tmp = new byte[len];
                random.nextBytes(tmp);
                System.arraycopy(tmp, 0, foo, start, len);
            }
    }
    
}
