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

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ssh.ErrorNotifiable;
import org.apache.commons.net.ssh.PacketHandler;
import org.apache.commons.net.ssh.connection.OpenFailException.Reason;
import org.apache.commons.net.ssh.transport.Transport;
import org.apache.commons.net.ssh.transport.TransportException;

public interface Channel extends Closeable, PacketHandler, ErrorNotifiable
{
    
    interface Direct extends Channel
    {
        
        void open() throws ConnectionException, TransportException;
        
    }
    
    interface Forwarded extends Channel
    {
        
        void confirm() throws TransportException;
        
        String getOriginatorIP();
        
        int getOriginatorPort();
        
        void reject(Reason reason, String message) throws TransportException;
        
    }
    
    void close() throws TransportException, ConnectionException;
    
    int getID();
    
    InputStream getInputStream();
    
    int getLocalMaxPacketSize();
    
    int getLocalWinSize();
    
    OutputStream getOutputStream();
    
    int getRecipient();
    
    int getRemoteMaxPacketSize();
    
    int getRemoteWinSize();
    
    Transport getTransport();
    
    String getType();
    
    boolean isOpen();
    
    void sendEOF() throws TransportException;
    
}
