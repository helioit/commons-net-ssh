package org.apache.commons.net.ssh.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pipe extends Thread
{
    
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
    
    public static void copy(InputStream in, OutputStream out, int bufSize, boolean closeStreamOnEOF) throws IOException
    {
        byte[] buf = new byte[bufSize];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
            out.flush();
        }
        if (closeStreamOnEOF)
            out.close();
    }
    
    protected final Logger log;
    protected final InputStream in;
    protected final OutputStream out;
    protected int bufSize = 1;
    protected boolean closeStreamOnEOF;
    
    protected ErrorCallback errCB;
    
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
    
    public Pipe closeOutputStreamOnEOF(boolean choice)
    {
        closeStreamOnEOF = choice;
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
        try {
            log.debug("Wil pipe from {} to {}", in, out);
            copy(in, out, bufSize, closeStreamOnEOF);
            log.debug("EOF on {}", in);
        } catch (IOException ioe) {
            log.error("In pipe from {} to {}: " + ioe.toString(), in, out);
            if (errCB != null)
                errCB.hadError(ioe);
        }
    }
    
}
