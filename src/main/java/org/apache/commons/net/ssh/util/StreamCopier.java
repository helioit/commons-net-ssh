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

public class StreamCopier extends Thread
{
    
    private static final Logger LOG = LoggerFactory.getLogger(StreamCopier.class);
    
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
    
    public static void copy(InputStream in, OutputStream out, int bufSize, boolean flush) throws IOException
    {
        byte[] buf = new byte[bufSize];
        int len;
        long count = 0;
        
        final long startTime = System.currentTimeMillis();
        
        while ((len = in.read(buf)) != -1)
        {
            out.write(buf, 0, len);
            count += len;
            if (flush)
                out.flush();
        }
        if (!flush)
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
    private boolean flush = true;
    
    private ErrorCallback errCB;
    
    public StreamCopier(String name, InputStream in, OutputStream out)
    {
        this.in = in;
        this.out = out;
        
        setName("streamCopier");
        log = LoggerFactory.getLogger(name);
    }
    
    public StreamCopier bufSize(int size)
    {
        bufSize = size;
        return this;
    }
    
    public StreamCopier flush(boolean choice)
    {
        flush = choice;
        return this;
    }
    
    public StreamCopier daemon(boolean choice)
    {
        setDaemon(choice);
        return this;
    }
    
    public StreamCopier errorCallback(ErrorCallback cb)
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
            copy(in, out, bufSize, flush);
            log.debug("EOF on {}", in);
        } catch (IOException ioe)
        {
            log.error("In pipe from {} to {}: " + ioe.toString(), in, out);
            if (errCB != null)
                errCB.hadError(ioe);
        }
    }
    
}