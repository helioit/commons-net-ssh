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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.net.ssh.SessionFactory;
import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.connection.ConnectionException;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.FileTransferUtil;
import org.apache.commons.net.ssh.util.IOUtils;

/**
 * Support for uploading files over a connected {@link SSHClient} link using SCP.
 */
public class SCPDownloadClient extends SCP
{
    
    private final ModeSetter modeSetter;
    
    private boolean recursive = true;
    
    public SCPDownloadClient(SessionFactory host)
    {
        this(host, null);
    }
    
    public SCPDownloadClient(SessionFactory host, ModeSetter modeSetter)
    {
        super(host);
        this.modeSetter = modeSetter == null ? new DefaultModeSetter() : modeSetter;
    }
    
    /**
     * Download a file from {@code sourcePath} on the connected host to {@code targetPath} locally.
     */
    @Override
    public synchronized int copy(String sourcePath, String targetPath) throws IOException
    {
        return super.copy(sourcePath, targetPath);
    }
    
    public void setRecursive(boolean recursive)
    {
        this.recursive = recursive;
    }
    
    void init(String source) throws ConnectionException, TransportException
    {
        List<String> args = new LinkedList<String>();
        addArg(args, Arg.SOURCE);
        addArg(args, Arg.QUIET);
        if (recursive)
            addArg(args, Arg.RECURSIVE);
        if (modeSetter.shouldPreserveTimes())
            addArg(args, Arg.PRESERVE_MODES);
        addArg(args, source == null || source.equals("") ? "." : source);
        execSCPWith(args);
    }
    
    long parseLong(String longString, String valType) throws SCPException
    {
        long val = 0;
        try
        {
            val = Long.parseLong(longString);
        } catch (NumberFormatException nfe)
        {
            throw new SCPException("Could not parse " + valType + " from `" + longString + "`", nfe);
        }
        return val;
    }
    
    /* e.g. "C0644" -> "644"; "D0755" -> "755" */
    String parsePermissions(String cmd) throws SCPException
    {
        if (cmd.length() != 5)
            throw new SCPException("Could not parse permissions from `" + cmd + "`");
        return cmd.substring(2);
    }
    
    void prepare(File f, String perms, String tMsg) throws IOException
    {
        modeSetter.setPermissions(f, perms);
        
        if (tMsg != null && modeSetter.shouldPreserveTimes())
        {
            String[] tMsgParts = tokenize(tMsg, 4); // e.g. T<mtime> 0 <atime> 0
            modeSetter.setLastModifiedTime(f, parseLong(tMsgParts[0].substring(1), "last modified time"));
            modeSetter.setLastAccessedTime(f, parseLong(tMsgParts[2], "last access time"));
        }
    }
    
    boolean process(String bufferedTMsg, String msg, File f) throws IOException
    {
        if (msg.length() < 1)
            throw new SCPException("Could not parse message `" + msg + "`");
        
        switch (msg.charAt(0))
        {
        
        case 'T':
            signal("ACK: T");
            process(msg, readMessage(true), f);
            break;
        
        case 'C':
            processFile(msg, bufferedTMsg, f);
            break;
        
        case 'D':
            processDirectory(msg, bufferedTMsg, f);
            break;
        
        case 'E':
            return true;
            
        case (char) 1:
            addWarning(msg.substring(1));
            break;
        
        case (char) 2:
            throw new SCPException("Remote SCP command returned error: " + msg.substring(1));
            
        default:
            String err = "Unrecognized message: `" + msg + "`";
            sendMessage((char) 2 + err);
            throw new SCPException(err);
        }
        
        return false;
    }
    
    void processDirectory(String dMsg, String tMsg, File f) throws IOException
    {
        String[] dMsgParts = tokenize(dMsg, 3); // e.g. D0755 0 <dirname>
        
        long length = parseLong(dMsgParts[1], "dir length");
        if (length != 0)
            throw new IOException("Remote SCP command sent strange directory length: " + length);
        
        f = FileTransferUtil.getTargetDirectory(f, dMsgParts[2]);
        prepare(f, parsePermissions(dMsgParts[0]), tMsg);
        
        signal("ACK: D");
        
        while (!process(null, readMessage(), f))
            ;
        
        signal("ACK: E");
    }
    
    void processFile(String cMsg, String tMsg, File f) throws IOException
    {
        String[] cMsgParts = tokenize(cMsg, 3);
        
        long length = parseLong(cMsgParts[1], "length");
        
        f = FileTransferUtil.getTargetFile(f, cMsgParts[2]);
        prepare(f, parsePermissions(cMsgParts[0]), tMsg);
        
        FileOutputStream fos = new FileOutputStream(f);
        signal("Remote can start transfer");
        transfer(scp.getInputStream(), fos, scp.getLocalMaxPacketSize(), length);
        check("Remote agrees transfer done");
        signal("Transfer done");
        IOUtils.closeQuietly(fos);
    }
    
    @Override
    void startCopy(String sourcePath, String targetPath) throws IOException
    {
        init(sourcePath);
        
        signal("Start status OK");
        
        String msg = readMessage(true);
        do
            process(null, msg, new File(targetPath));
        while ((msg = readMessage(false)) != null);
    }
    
    String[] tokenize(String msg, int numPartsExpected) throws IOException
    {
        String[] parts = msg.split(" ");
        if (parts.length != numPartsExpected)
            throw new IOException("Could not parse message received from remote SCP: " + msg);
        return parts;
    }
    
}