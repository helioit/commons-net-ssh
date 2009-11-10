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

import org.apache.commons.net.ssh.transport.Transport;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Constants.DisconnectReason;
import org.apache.commons.net.ssh.util.Constants.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract class for {@link Service} that implements common or default functionality.
 */
public abstract class AbstractService implements Service
{
    
    /** Logger */
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    /** Assigned name of this service */
    protected final String name;
    /** Transport layer */
    protected final Transport trans;
    /** Timeout for blocking operations */
    protected int timeout;
    
    public AbstractService(String name, Transport trans)
    {
        this.name = name;
        this.trans = trans;
        timeout = trans.getTimeout();
    }
    
    public String getName()
    {
        return name;
    }
    
    public int getTimeout()
    {
        return this.timeout;
    }
    
    public Transport getTransport()
    {
        return trans;
    }
    
    public void handle(Message msg, Buffer buf) throws SSHException
    {
        trans.sendUnimplemented();
    }
    
    public void notifyError(SSHException error)
    {
        log.debug("Was notified of {}", error.toString());
    }
    
    public void notifyUnimplemented(long seqNum) throws SSHException
    {
        throw new SSHException(DisconnectReason.PROTOCOL_ERROR, "Unexpected: SSH_MSG_UNIMPLEMENTED");
    }
    
    public void request() throws TransportException
    {
        final Service active = trans.getService();
        if (!equals(active))
            if (name.equals(active.getName()))
                trans.setService(this);
            else
                trans.reqService(this);
    }
    
    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }
    
    public void notifyDisconnect() throws SSHException
    {
        log.debug("Was notified of disconnect");
    }
    
}
