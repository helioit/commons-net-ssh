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
package org.apache.commons.net.ssh.scp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.connection.ConnectionException;
import org.apache.commons.net.ssh.connection.Session.Command;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 * 
 * @see <a href="http://blogs.sun.com/janp/entry/how_the_scp_protocol_works">SCP Protocol</a>
 */
public abstract class SCP
{
    
    public static class SCPException extends SSHException
    {
        public SCPException(String message)
        {
            super(message);
        }
        
        public SCPException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
    
    protected static enum Arg
    {
        SOURCE('f'), SINK('t'), RECURSIVE('r'), VERBOSE('v'), PRESERVE_MODES('p'), QUIET('q');
        
        private char a;
        
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
    protected static final String SCP_COMMAND = "scp";
    
    protected static void addArg(List<String> args, Arg arg)
    {
        addArg(args, arg.toString());
    }
    
    protected static void addArg(List<String> args, String arg)
    {
        args.add(arg);
    }
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    protected final SSHClient host;
    
    protected final Queue<String> warnings = new LinkedList<String>();
    protected int exitStatus;
    
    protected Command scp;
    
    protected SCP(SSHClient host)
    {
        this.host = host;
    }
    
    public synchronized int copy(String sourcePath, String targetPath) throws IOException
    {
        cleanSlate();
        try {
            startCopy(sourcePath, targetPath);
        } finally {
            exit();
        }
        return exitStatus;
    }
    
    public int getExitStatus()
    {
        return exitStatus;
    }
    
    public Queue<String> getWarnings()
    {
        return warnings;
    }
    
    public boolean hadWarnings()
    {
        return !warnings.isEmpty();
    }
    
    void addWarning(String warning)
    {
        log.warn(warning);
        warnings.add(warning);
    }
    
    void check(String what) throws IOException
    {
        int code = scp.getInputStream().read();
        switch (code)
        {
        case -1:
            String stderr = scp.getErrorAsString();
            if (stderr != "")
                stderr = ". Additional info: `" + stderr + "`";
            throw new SCPException("EOF while expecting response to protocol message" + stderr);
        case 0: // OK
            log.debug(what);
            return;
        case 1:
            addWarning(readMessage());
            break;
        case 2:
            throw new SCPException("Remote SCP command had error: " + readMessage());
        default:
            throw new SCPException("Received unknown response code");
        }
    }
    
    void cleanSlate()
    {
        exitStatus = -1;
        warnings.clear();
    }
    
    void execSCPWith(List<String> args) throws ConnectionException, TransportException
    {
        String cmd = SCP_COMMAND;
        for (String arg : args)
            cmd += " " + arg;
        scp = host.startSession().exec(cmd);
    }
    
    void exit()
    {
        if (scp != null) {
            
            IOUtils.closeQuietly(scp);
            
            if (scp.getExitStatus() != null) {
                exitStatus = scp.getExitStatus();
                if (scp.getExitStatus() != 0)
                    log.warn("SCP exit status: {}", scp.getExitStatus());
            } else
                exitStatus = -1;
            
            if (scp.getExitSignal() != null)
                log.warn("SCP exit signal: {}", scp.getExitSignal());
        }
        
        scp = null;
    }
    
    String readMessage() throws IOException
    {
        return readMessage(true);
    }
    
    String readMessage(boolean errOnEOF) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        int x;
        while ((x = scp.getInputStream().read()) != LF)
            if (x == -1) {
                if (errOnEOF)
                    throw new IOException("EOF while reading message");
                else
                    return null;
            } else
                sb.append((char) x);
        log.debug("Read message: {}", sb);
        return sb.toString();
    }
    
    void sendMessage(String msg) throws IOException
    {
        log.debug("Sending message: {}", msg);
        scp.getOutputStream().write((msg + LF).getBytes());
        scp.getOutputStream().flush();
        check("Message ACK received");
    }
    
    void signal(String what) throws IOException
    {
        log.debug("Signalling: {}", what);
        scp.getOutputStream().write(0);
        scp.getOutputStream().flush();
    }
    
    abstract void startCopy(String sourcePath, String targetPath) throws IOException;
    
    void transfer(InputStream in, OutputStream out, int bufSize, long len) throws IOException
    {
        final byte[] buf = new byte[bufSize];
        long count = 0;
        int read;
        
        long startTime = System.currentTimeMillis();
        
        while ((read = in.read(buf, 0, (int) Math.min(bufSize, len - count))) != -1 && count < len) {
            out.write(buf, 0, read);
            count += read;
        }
        out.flush();
        
        final long sizeKiB = count / 1024;
        final double timeSeconds = (System.currentTimeMillis() - startTime) / 1000.0;
        log.info(sizeKiB / 1024.0 + " MiB transferred  in {} seconds ({} KiB/s)", timeSeconds, (sizeKiB / timeSeconds));
        
        if (read == -1)
            throw new IOException("Had EOF before transfer completed");
    }
    
}
