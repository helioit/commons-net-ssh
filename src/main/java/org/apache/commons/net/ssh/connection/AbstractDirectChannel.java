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

import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Constants.Message;

/**
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public abstract class AbstractDirectChannel extends AbstractChannel implements Channel.Direct
{
    
    protected AbstractDirectChannel(String name, Connection conn)
    {
        super(name, conn);
        
        /*
         * We expect to receive channel open confirmation/rejection and want to be able to handle
         * this packet.
         */
        conn.attach(this);
    }
    
    public void open() throws ConnectionException, TransportException
    {
        lock.lock();
        try {
            trans.writePacket(buildOpenReq());
            open.await(conn.getTimeout());
        } finally {
            lock.unlock();
        }
    }
    
    protected Buffer buildOpenReq()
    {
        return new Buffer(Message.CHANNEL_OPEN) //
                                               .putString(type) //
                                               .putInt(id) //
                                               .putInt(lwin.getSize()) //
                                               .putInt(lwin.getMaxPacketSize());
    }
    
    protected void gotOpenConfirmation(Buffer buf)
    {
        init(buf.getInt(), buf.getInt(), buf.getInt());
        open.set();
    }
    
    protected void gotOpenFailure(Buffer buf)
    {
        open.error(new OpenFailException(type, buf.getInt(), buf.getString()));
        conn.forget(this);
    }
    
    @Override
    protected void gotUnknown(Message cmd, Buffer buf) throws ConnectionException, TransportException
    {
        switch (cmd)
        {
        
        case CHANNEL_OPEN_CONFIRMATION:
            gotOpenConfirmation(buf);
            break;
        
        case CHANNEL_OPEN_FAILURE:
            gotOpenFailure(buf);
            break;
        
        default:
            super.gotUnknown(cmd, buf);
        }
    }
    
}
