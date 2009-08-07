package org.apache.commons.net.ssh.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author shikhar
 */
public class ChannelInputStream extends InputStream
{
    
    protected final Channel chan;
    protected final LocalWindow win;
    
    protected final Buffer buf;
    protected final byte[] b = new byte[1];
    
    protected boolean eof;
    
    public ChannelInputStream(Channel chan, LocalWindow win)
    {
        this.chan = chan;
        this.win = win;
        
        buf = new Buffer(chan.getLocalMaxPacketSize());
    }
    
    @Override
    public int available()
    {
        synchronized (buf) {
            return buf.available();
        }
    }
    
    @Override
    public void close()
    {
        eof();
    }
    
    @Override
    public int read() throws IOException
    {
        synchronized (b) {
            return read(b, 0, 1) == -1 ? -1 : b[0];
        }
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        int avail;
        synchronized (buf) {
            while (buf.available() == 0 && !eof)
                try {
                    buf.wait();
                } catch (InterruptedException e) {
                    throw (IOException) new InterruptedIOException().initCause(e);
                }
            if (eof)
                return -1;
            if (len > buf.available())
                len = buf.available();
            buf.getRawBytes(b, off, len = buf.available() > len ? len : len);
            if (buf.rpos() > win.getMaxPacketSize() || buf.available() == 0)
                buf.compact();
            avail = win.getInitialSize() - buf.available();
        }
        win.check(avail);
        return len;
    }
    
    public void receive(byte[] data, int offset, int len) throws ConnectionException, TransportException
    {
        synchronized (buf) {
            if (eof)
                throw new ConnectionException("Getting data on EOF'ed stream");
            buf.putRawBytes(data, offset, len);
            buf.notifyAll();
        }
        win.consume(len);
    }
    
    @Override
    public String toString()
    {
        return "< ChannelInputStream for Channel #" + chan.getID() + " >";
    }
    
    protected void eof()
    {
        synchronized (buf) {
            if (!eof) {
                eof = true;
                buf.notifyAll();
            }
        }
    }
}
