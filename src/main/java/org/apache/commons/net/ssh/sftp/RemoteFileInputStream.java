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
package org.apache.commons.net.ssh.sftp;

import java.io.IOException;
import java.io.InputStream;

public class RemoteFileInputStream extends InputStream
{
    
    private final byte[] b = new byte[1];
    
    private final RemoteFile rf;
    
    private long fileOffset;
    private long markPos;
    private long readLimit;
    
    public RemoteFileInputStream(RemoteFile rf)
    {
        this(rf, 0);
    }
    
    public RemoteFileInputStream(RemoteFile rf, int fileOffset)
    {
        this.rf = rf;
        this.fileOffset = fileOffset;
    }
    
    @Override
    public boolean markSupported()
    {
        return true;
    }
    
    @Override
    public void mark(int readLimit)
    {
        this.readLimit = readLimit;
        markPos = fileOffset;
    }
    
    @Override
    public void reset() throws IOException
    {
        fileOffset = markPos;
    }
    
    @Override
    public long skip(long n) throws IOException
    {
        return (this.fileOffset = Math.min(fileOffset + n, rf.length()));
    }
    
    @Override
    public int read() throws IOException
    {
        return read(b, 0, 1) == -1 ? -1 : b[0];
    }
    
    @Override
    public int read(byte[] into, int off, int len) throws IOException
    {
        int read = rf.read(fileOffset, into, off, len);
        if (read != -1)
        {
            fileOffset += read;
            if (markPos != 0 && read > readLimit) // Invalidate mark position
                markPos = 0;
        }
        return read;
    }
    
}
