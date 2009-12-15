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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.net.ssh.ErrorNotifiable;
import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.SSHPacket;
import org.apache.commons.net.ssh.util.Constants.Message;

/**
 * {@link OutputStream} for channels. Buffers data upto the remote window's maximum packet size. Data can also be
 * flushed via {@link #flush()} and is also flushed on {@link #close()}.
 */
public class ChannelOutputStream extends OutputStream implements ErrorNotifiable
{
    
    private final Channel chan;
    private final RemoteWindow win;
    private final SSHPacket buffer = new SSHPacket();
    private final byte[] b = new byte[1];
    private int bufferLength;
    private boolean closed;
    private SSHException error;
    
    public ChannelOutputStream(Channel chan, RemoteWindow win)
    {
        this.chan = chan;
        this.win = win;
        prepBuffer();
    }
    
    @Override
    public synchronized void close() throws IOException
    {
        if (!closed)
            try
            {
                flush();
                chan.sendEOF();
            } finally
            {
                setClosed();
            }
    }
    
    @Override
    public synchronized void flush() throws IOException
    {
        checkClose();
        
        if (bufferLength <= 0) // No data to send
            return;
        
        putRecipientAndLength();
        
        try
        {
            win.waitAndConsume(bufferLength);
            chan.getTransport().write(buffer);
        } finally
        {
            prepBuffer();
        }
    }
    
    public synchronized void notifyError(SSHException error)
    {
        this.error = error;
    }
    
    public synchronized void setClosed()
    {
        closed = true;
    }
    
    @Override
    public synchronized void write(byte[] data, int off, int len) throws IOException
    {
        checkClose();
        while (len > 0)
        {
            int x = Math.min(len, win.getMaxPacketSize() - bufferLength);
            if (x <= 0)
            {
                flush();
                continue;
            }
            buffer.putRawBytes(data, off, x);
            bufferLength += x;
            off += x;
            len -= x;
        }
    }
    
    @Override
    public synchronized void write(int w) throws IOException
    {
        b[0] = (byte) w;
        write(b, 0, 1);
    }
    
    private void checkClose() throws SSHException
    {
        if (closed)
            if (error != null)
                throw error;
            else
                throw new ConnectionException("Stream closed");
    }
    
    private void prepBuffer()
    {
        bufferLength = 0;
        buffer.rpos(5);
        buffer.wpos(5);
        buffer.putMessageID(Message.CHANNEL_DATA);
        buffer.putInt(0); // meant to be recipient
        buffer.putInt(0); // meant to be data length
    }
    
    private void putRecipientAndLength()
    {
        int origPos = buffer.wpos();
        buffer.wpos(6);
        buffer.putInt(chan.getRecipient());
        buffer.putInt(bufferLength);
        buffer.wpos(origPos);
    }
    
    @Override
    public String toString()
    {
        return "< ChannelOutputStream for Channel #" + chan.getID() + " >";
    }
    
}