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
 * 
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
        
        SOURCE('f'), SINK('t'), RECURSIVE('r'), VERBOSE('v'), MODE_PRESERVING('p');
        
        char x;
        
        private Arg(char x)
        {
            this.x = x;
        }
        
        @Override
        public String toString()
        {
            return "-" + x;
        }
        
    }
    
    protected static final char LF = '\n';
    protected static final String COMMAND = "scp";
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final SSHClient host;
    
    protected Command scp;
    
    public SCPClient(SSHClient host)
    {
        this.host = host;
    }
    
    public abstract int copy(String source, String target) throws IOException;
    
    protected void checkResponseOK() throws IOException
    {
        int code = scp.getInputStream().read();
        log.debug("Response code: {}", code);
        switch (code)
        {
            case -1:
                String err = scp.getErrorAsString();
                if (err != "")
                    throw new SCPException("Remote SCP command returned error: " + err);
                else
                    throw new SCPException("EOF while expecting response to protocol message");
            case 0: // OK
                return;
            case 1:
                log.warn("Remote SCP warning: {}", readMessage());
                break;
            case 2:
                throw new SCPException("Remote SCP command returned error: " + readMessage());
            default:
                throw new SCPException("Received unknown response code");
        }
    }
    
    protected void doCopy(InputStream in, OutputStream out, int bufSize, long len) throws IOException
    {
        byte[] buf = new byte[bufSize];
        long count = 0;
        int read;
        while ((read = in.read(buf)) != -1 && count < len) {
            out.write(buf, 0, read);
            out.flush();
            count += read;
        }
        sendOK();
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
            if (scp.getExitStatus() != 0)
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
        char ch;
        StringBuilder sb = new StringBuilder();
        while ((ch = (char) scp.getInputStream().read()) != LF) {
            if (ch == -1) {
                scp.close();
                throw new SCPException("EOF while reading error message");
            }
            sb.append(ch);
        }
        return sb.toString();
    }
    
    protected void sendMessage(String msg) throws IOException
    {
        log.debug("Sending message: {}", msg);
        scp.getOutputStream().write((msg + LF).getBytes());
    }
    
    protected void sendOK() throws IOException
    {
        log.debug("Sending OK");
        scp.getOutputStream().write(0);
    }
    
}
