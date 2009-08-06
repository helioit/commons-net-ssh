package org.apache.commons.net.ssh.scp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.connection.ConnectionException;
import org.apache.commons.net.ssh.connection.Session.Command;
import org.apache.commons.net.ssh.transport.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shikhar
 * @see <a href="http://blogs.sun.com/janp/entry/how_the_scp_protocol_works">SCP Protocol</a>
 */
public abstract class SCPClient
{
    
    public static class SCPException extends SSHException
    {
        public SCPException(String message)
        {
            super(message);
        }
    }
    
    protected static enum Arg
    {
        SOURCE('f'), SINK('t'), RECURSIVE('r'), VERBOSE('v'), PRESERVE_MODES('p'), QUIET('q');
        
        char a;
        
        private Arg(char a)
        {
            this.a = a;
        }
        
        @Override
        public String toString()
        {
            return "-" + a;
        }
    }
    
    protected static final char LF = '\n';
    protected static final String COMMAND = "scp";
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final SSHClient host;
    
    protected Command scp;
    
    protected SCPClient(SSHClient host)
    {
        this.host = host;
    }
    
    public synchronized int copy(String sourcePath, String targetPath) throws IOException
    {
        int ret = -1;
        try {
            startCopy(sourcePath, targetPath);
        } finally {
            ret = exit();
        }
        return ret;
    }
    
    protected void checkResponseOK() throws IOException
    {
        int code = scp.getInputStream().read();
        log.debug("Response code: {}", code);
        switch (code)
        {
            case -1:
                String stderr = scp.getErrorAsString();
                if (stderr != "")
                    stderr = ". Additional info: " + stderr;
                else
                    throw new SCPException("EOF while expecting response to protocol message" + stderr);
            case 0: // OK
                return;
            case 1:
            case 2:
                throw new SCPException("Remote SCP command had error: " + readMessage());
            default:
                throw new SCPException("Received unknown response code");
        }
    }
    
    protected synchronized void execSCPWith(List<String> args) throws ConnectionException, TransportException
    {
        String cmd = COMMAND;
        for (String arg : args)
            cmd += " " + arg;
        scp = host.startSession().exec(cmd);
    }
    
    protected synchronized int exit() throws IOException
    {
        if (scp != null) {
            scp.close();
            if (scp.getExitStatus() != null && scp.getExitStatus() != 0)
                log.warn("SCP exit status: {}", scp.getExitStatus());
            if (scp.getExitSignal() != null)
                log.warn("SCP exit signal: {}", scp.getExitSignal());
            return scp.getExitStatus() != null ? scp.getExitStatus() : -1;
        }
        scp = null;
        return -1;
    }
    
    protected String readMessage() throws IOException
    {
        StringBuilder sb = new StringBuilder();
        char ch;
        while ((ch = (char) scp.getInputStream().read()) != LF) {
            if (ch == -1) {
                scp.close();
                throw new SCPException("EOF while reading message");
            }
            sb.append(ch);
        }
        log.debug("Read message: {}", sb);
        return sb.toString();
    }
    
    protected void sendMessage(String msg) throws IOException
    {
        log.debug("Sending message: {}", msg);
        scp.getOutputStream().write((msg + LF).getBytes());
        scp.getOutputStream().flush();
        checkResponseOK();
    }
    
    protected void sendOK() throws IOException
    {
        log.debug("Sending OK");
        scp.getOutputStream().write(0);
        scp.getOutputStream().flush();
    }
    
    protected abstract void startCopy(String sourcePath, String targetPath) throws IOException;
    
    protected void transfer(InputStream in, OutputStream out, int bufSize, long len) throws IOException
    {
        byte[] buf = new byte[bufSize];
        long count = 0;
        int read;
        long startTime = System.nanoTime();
        while ((read = in.read(buf, 0, (int) Math.min(bufSize, len - count))) != -1 && count < len) {
            out.write(buf, 0, read);
            out.flush();
            count += read;
        }
        log.info("Transferred @ {} KiB/s", (count / (1024 * (System.nanoTime() - startTime) / 1000000000.0)));
        if (read == -1 && !(count == len))
            throw new IOException("Had EOF before transfer completed");
    }
    
}
