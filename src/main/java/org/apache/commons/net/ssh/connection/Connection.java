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
package org.apache.commons.net.ssh.connection;

import org.apache.commons.net.ssh.connection.OpenFailException.Reason;
import org.apache.commons.net.ssh.transport.Transport;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Future;

public interface Connection
{
    
    public Future<Buffer, ConnectionException> sendGlobalRequest(String name, boolean wantReply, Buffer specifics)
            throws TransportException;
    
    void attach(Channel chan);
    
    void attach(ForwardedChannelOpener handler);
    
    void forget(Channel chan);
    
    void forget(ForwardedChannelOpener handler);
    
    Channel get(int id);
    
    ForwardedChannelOpener get(String chanType);
    
    int getMaxPacketSize();
    
    int getTimeout();
    
    Transport getTransport();
    
    int getWindowSize();
    
    void join() throws InterruptedException;
    
    int nextID();
    
    void sendOpenFailure(int recipient, Reason reason, String message) throws TransportException;
    
    void setMaxPacketSize(int maxPacketSize);
    
    void setTimeout(int timeout);
    
    void setWindowSize(int windowSize);
}
