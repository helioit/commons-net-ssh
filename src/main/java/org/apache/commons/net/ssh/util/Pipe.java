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
package org.apache.commons.net.ssh.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pipe extends Thread
{
    
    private static final Logger LOG = LoggerFactory.getLogger(Pipe.class);
    
    public interface EOFCallback
    {
        void hadEOF();
    }
    
    public interface ErrorCallback
    {
        void hadError(IOException e);
    }
    
    public static ErrorCallback closeOnErrorCallback(final Closeable closable)
    {
        return new ErrorCallback()
        {
            public void hadError(IOException ioe)
            {
                IOUtils.closeQuietly(closable);
            }
        };
    }
    
    public static void pipe(InputStream in, OutputStream out, int bufSize) throws IOException
    {
        pipe(in, out, bufSize, true);
    }
    
    public static void pipe(InputStream in, OutputStream out, int bufSize, boolean dontFlushEveryWrite)
            throws IOException
    {
        byte[] buf = new byte[bufSize];
        int len;
        long count = 0;
        
        final long startTime = System.currentTimeMillis();
        
        while ((len = in.read(buf)) != -1)
        {
            out.write(buf, 0, len);
            count += len;
            if (!dontFlushEveryWrite)
                out.flush();
        }
        if (dontFlushEveryWrite)
            out.flush();
        
        final float sizeKiB = count / 1024;
        final double timeSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        LOG.info(sizeKiB + " KiB transferred  in {} seconds ({} KiB/s)", timeSeconds, (sizeKiB / timeSeconds));
        
        in.close();
        out.close();
    }
    
    private final Logger log;
    private final InputStream in;
    private final OutputStream out;
    private int bufSize = 1;
    private boolean dontFlushEveryWrite;
    
    private ErrorCallback errCB;
    
    public Pipe(String name, InputStream in, OutputStream out)
    {
        this.in = in;
        this.out = out;
        
        setName("pipe");
        log = LoggerFactory.getLogger(name);
    }
    
    public Pipe bufSize(int size)
    {
        bufSize = size;
        return this;
    }
    
    public Pipe dontFlushEveryWrite(boolean choice)
    {
        dontFlushEveryWrite = choice;
        return this;
    }
    
    public Pipe daemon(boolean choice)
    {
        setDaemon(choice);
        return this;
    }
    
    public Pipe errorCallback(ErrorCallback cb)
    {
        errCB = cb;
        return this;
    }
    
    @Override
    public void run()
    {
        try
        {
            log.debug("Wil pipe from {} to {}", in, out);
            pipe(in, out, bufSize, dontFlushEveryWrite);
            log.debug("EOF on {}", in);
        } catch (IOException ioe)
        {
            log.error("In pipe from {} to {}: " + ioe.toString(), in, out);
            if (errCB != null)
                errCB.hadError(ioe);
        }
    }
    
}