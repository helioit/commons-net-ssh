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

import org.apache.commons.net.ssh.ErrorNotifiable;
import org.apache.commons.net.ssh.HostKeyVerifier;
import org.apache.commons.net.ssh.PacketHandler;

public interface KeyExchanger extends PacketHandler, ErrorNotifiable
{
    
    /**
     * Add a callback for host key verification.
     * <p>
     * Any of the {@link HostKeyVerifier} implementations added this way can deem a host key to be
     * acceptable, allowing the connection to proceed. Otherwise, a {@link TransportException} will
     * result during session initialization.
     * 
     * @param hkv
     *            object whose {@link HostKeyVerifier#verify} method will be invoked
     */
    void addHostKeyVerifier(HostKeyVerifier hkv);
    
    /**
     * Returns the session identifier computed during key exchange.
     * <p>
     * If the session has not yet been initialized via {@link #open}, it will be {@code null}.
     * 
     * @return session identifier as a byte array
     */
    byte[] getSessionID();
    
    void init(Transport trans);
    
    boolean isKexOngoing();
    
    void startKex(boolean waitForDone) throws TransportException;
    
    void waitForDone() throws TransportException;
    
}