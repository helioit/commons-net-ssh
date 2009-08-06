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
import java.io.InterruptedIOException;
import java.io.OutputStream;

import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Constants.Message;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public class ChannelOutputStream extends OutputStream
{
    
    protected final Channel chan;
    protected final RemoteWindow win;
    
    protected final byte[] b = new byte[1];
    
    protected Buffer buffer;
    protected int bufferLength;
    protected boolean closed;
    
    public ChannelOutputStream(Channel chan, RemoteWindow win)
    {
        this.chan = chan;
        this.win = win;
    }
    
    @Override
    public synchronized void close() throws IOException
    {
        if (!closed)
            try {
                flush();
            } finally {
                closed = true;
                chan.sendEOF();
            }
    }
    
    @Override
    public synchronized void flush() throws IOException
    {
        if (closed)
            throw new ConnectionException("Stream closed");
        int pos = buffer.wpos();
        if (bufferLength <= 0)
            // No data to send
            return;
        buffer.wpos(10);
        buffer.putInt(bufferLength);
        buffer.wpos(pos);
        try {
            win.waitAndConsume(bufferLength);
            chan.getTransport().writePacket(buffer);
        } catch (InterruptedException e) {
            throw (IOException) new InterruptedIOException().initCause(e);
        } finally {
            newBuffer();
        }
    }
    
    public void init()
    {
        newBuffer();
    }
    
    @Override
    public String toString()
    {
        return "< ChannelOutputStream for Channel #" + chan.getID() + " >";
    }
    
    @Override
    public synchronized void write(byte[] data, int off, int len) throws IOException
    {
        if (closed)
            throw new ConnectionException("Stream closed");
        while (len > 0) {
            int x = Math.min(len, win.getMaxPacketSize() - bufferLength);
            if (x <= 0) {
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
    
    private void newBuffer()
    {
        buffer = new Buffer(Message.CHANNEL_DATA);
        buffer.putInt(chan.getRecipient());
        buffer.putInt(0);
        bufferLength = 0;
    }
    
    synchronized boolean isClosed()
    {
        return closed;
    }
    
}
