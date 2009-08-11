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
package org.apache.commons.net.ssh.userauth;

import org.apache.commons.net.ssh.PacketHandler;
import org.apache.commons.net.ssh.transport.TransportException;

/**
 * An authentication method of the <a href="http://www.ietf.org/rfc/rfc4252.txt">SSH Authentication
 * Protocol</a>.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 * @see UserAuth
 */
public interface AuthMethod extends PacketHandler
{
    
    /**
     * Returns assigned name of this authentication method
     */
    String getName();
    
    /**
     * Initializes this {@link AuthMethod} with the {@link AuthParams parameters} needed for
     * authentication. This method must be called before requesting authentication with this method.
     */
    void init(AuthParams params);
    
    /**
     * 
     * @throws UserAuthException
     * @throws TransportException
     */
    void request() throws UserAuthException, TransportException;
    
    /**
     * Returns whether authentication should be reattempted if it failed.
     */
    boolean shouldRetry();
    
}
