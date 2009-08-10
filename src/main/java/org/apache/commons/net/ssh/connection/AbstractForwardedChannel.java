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
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Constants.Message;

//TODO: move to ConnProto
public abstract class AbstractForwardedChannel extends AbstractChannel implements Channel.Forwarded
{
    
    protected final String origIP;
    protected final int origPort;
    
    protected AbstractForwardedChannel(String name, Connection conn, int recipient, int remoteWinSize,
            int remoteMaxPacketSize, String origIP, int origPort)
    {
        super(name, conn);
        this.origIP = origIP;
        this.origPort = origPort;
        init(recipient, remoteWinSize, remoteMaxPacketSize);
    }
    
    public void confirm() throws TransportException
    {
        log.info("Confirming `{}` channel #{}", type, id);
        trans.writePacket(newBuffer(Message.CHANNEL_OPEN_CONFIRMATION) //
                                                                      .putInt(id) //
                                                                      .putInt(lwin.getSize()) //
                                                                      .putInt(lwin.getMaxPacketSize()));
        open.set();
        conn.attach(this);
    }
    
    public String getOriginatorIP()
    {
        return origIP;
    }
    
    public int getOriginatorPort()
    {
        return origPort;
    }
    
    public void reject(Reason reason, String message) throws TransportException
    {
        log.info("Rejecting `{}` channel: {}", type, message);
        conn.sendOpenFailure(recipient, reason, message);
    }
    
}
