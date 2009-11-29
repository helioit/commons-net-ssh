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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.net.ssh.SessionFactory;
import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.connection.ConnectionException;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.IOUtils;

/**
 * Support for uploading files over a connected {@link SSHClient} link using SCP.
 */
public class SCPUploadClient extends SCPEngine
{
    
    private final ModeGetter modeGetter;
    private FileFilter fileFilter;
    
    public SCPUploadClient(SessionFactory host)
    {
        this(host, null);
    }
    
    public SCPUploadClient(SessionFactory host, ModeGetter modeGetter)
    {
        super(host);
        this.modeGetter = modeGetter == null ? new DefaultModeGetter() : modeGetter;
    }
    
    /**
     * Upload a file from {@code sourcePath} locally to {@code targetPath} on the remote host.
     */
    @Override
    public synchronized int copy(String sourcePath, String targetPath) throws IOException
    {
        return super.copy(sourcePath, targetPath);
    }
    
    public void setFileFilter(FileFilter fileFilter)
    {
        this.fileFilter = fileFilter;
    }
    
    File[] getChildren(File f) throws IOException
    {
        File[] files = fileFilter == null ? f.listFiles() : f.listFiles(fileFilter);
        if (files == null)
            throw new IOException("Error listing files in directory: " + f);
        return files;
    }
    
    void init(String target) throws ConnectionException, TransportException
    {
        List<String> args = new LinkedList<String>();
        addArg(args, Arg.SINK);
        addArg(args, Arg.RECURSIVE);
        if (modeGetter.shouldPreserveTimes())
            addArg(args, Arg.PRESERVE_MODES);
        args.add(target == null || target.equals("") ? "." : target);
        execSCPWith(args);
    }
    
    void process(File f) throws IOException
    {
        if (f.isDirectory())
            sendDirectory(f);
        else if (f.isFile())
            sendFile(f);
        else
            throw new IOException(f + " is not a regular file or directory");
    }
    
    void sendDirectory(File f) throws IOException
    {
        log.info("Entering directory `{}`", f.getName());
        if (modeGetter.shouldPreserveTimes())
            sendMessage("T" + modeGetter.getLastModifiedTime(f) + " 0 " + modeGetter.getLastAccessTime(f) + " 0");
        sendMessage("D" + modeGetter.getPermissions(f) + " 0 " + f.getName());
        
        for (File child : getChildren(f))
            process(child);
        
        sendMessage("E");
        log.info("Exiting directory `{}`", f.getName());
    }
    
    void sendFile(File f) throws IOException
    {
        log.info("Sending `{}`...", f.getName());
        if (modeGetter.shouldPreserveTimes())
            sendMessage("T" + modeGetter.getLastModifiedTime(f) + " 0 " + modeGetter.getLastAccessTime(f) + " 0");
        InputStream src = new FileInputStream(f);
        sendMessage("C" + modeGetter.getPermissions(f) + " " + f.length() + " " + f.getName());
        transfer(src, scp.getOutputStream(), scp.getRemoteMaxPacketSize(), f.length());
        signal("Transfer done");
        check("Remote agrees transfer done");
        IOUtils.closeQuietly(src);
    }
    
    @Override
    synchronized void startCopy(String sourcePath, String targetPath) throws IOException
    {
        init(targetPath);
        check("Start status OK");
        process(new File(sourcePath));
    }
    
}
