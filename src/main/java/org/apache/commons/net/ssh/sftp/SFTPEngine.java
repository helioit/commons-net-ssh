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
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.net.ssh.SessionFactory;
import org.apache.commons.net.ssh.connection.ConnectionException;
import org.apache.commons.net.ssh.connection.Session.Subsystem;
import org.apache.commons.net.ssh.sftp.Response.StatusCode;
import org.apache.commons.net.ssh.transport.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SFTPEngine
{
    
    /** Logger */
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final int PROTOCOL_VERSION = 3;
    
    public static final int DEFAULT_TIMEOUT = 30;
    
    private volatile int timeout = DEFAULT_TIMEOUT;
    
    private final Subsystem sub;
    private final PacketReader reader;
    private final OutputStream out;
    
    private long reqID;
    private int negotiatedVersion;
    private final Map<String, String> serverExtensions = new HashMap<String, String>();
    
    public SFTPEngine(SessionFactory ssh) throws ConnectionException, TransportException
    {
        sub = ssh.startSession().startSubsystem("sftp");
        out = sub.getOutputStream();
        reader = new PacketReader(sub.getInputStream());
    }
    
    public Subsystem getSubsystem()
    {
        return sub;
    }
    
    public SFTPEngine init() throws IOException
    {
        SFTPPacket<Request> pk = new SFTPPacket<Request>(PacketType.INIT).putInt(PROTOCOL_VERSION);
        transmit(pk);
        
        SFTPPacket<Response> response = reader.readPacket();
        PacketType type = response.readType();
        if (type != PacketType.VERSION)
            throw new SFTPException("Expected INIT packet, received: " + type);
        negotiatedVersion = response.readInt();
        log.info("Client version {}, server version {}", PROTOCOL_VERSION, negotiatedVersion);
        if (negotiatedVersion < PROTOCOL_VERSION)
            throw new SFTPException("Server reported protocol version: " + negotiatedVersion);
        
        while (response.available() > 0)
            serverExtensions.put(response.readString(), response.readString());
        
        // Start reader thread
        reader.start();
        return this;
    }
    
    public int getOperativeProtocolVersion()
    {
        return negotiatedVersion;
    }
    
    public synchronized Request newRequest(PacketType type)
    {
        return new Request(type, reqID = reqID + 1 & 0xffffffffL);
    }
    
    private synchronized void transmit(SFTPPacket<Request> payload) throws IOException
    {
        final int len = payload.available();
        out.write((len >>> 24) & 0xff);
        out.write((len >>> 16) & 0xff);
        out.write((len >>> 8) & 0xff);
        out.write(len & 0xff);
        out.write(payload.array(), 0, len);
        out.flush();
    }
    
    public Response make(Request req) throws IOException
    {
        reader.expectResponseTo(req);
        log.debug("Sending {}", req);
        transmit(req);
        return req.getFuture().get(timeout);
    }
    
    public RemoteFile open(String path, Set<OpenMode> modes, FileAttributes fa) throws IOException
    {
        final String handle = make(newRequest(PacketType.OPEN) //
                .putString(path) //
                .putInt(OpenMode.toMask(modes)) //
                .putFileAttributes(fa) // 
        ).ensurePacketTypeIs(PacketType.HANDLE).readString();
        return new RemoteFile(this, path, handle);
    }
    
    public RemoteFile open(String filename, Set<OpenMode> modes) throws IOException
    {
        return open(filename, modes, new FileAttributes());
    }
    
    public RemoteFile open(String filename) throws IOException
    {
        return open(filename, EnumSet.of(OpenMode.READ));
    }
    
    public RemoteDir openDir(String path) throws IOException
    {
        final String handle = make(newRequest(PacketType.OPENDIR).putString(path)) //
                .ensurePacketTypeIs(PacketType.HANDLE).readString();
        return new RemoteDir(this, path, handle);
    }
    
    public void setAttributes(String path, FileAttributes attrs) throws IOException
    {
        make(newRequest(PacketType.SETSTAT) //
                .putString(path) //
                .putFileAttributes(attrs) //
        ).ensureStatusOK();
    }
    
    public String readLink(String path) throws IOException
    {
        return readSingleName(make(newRequest(PacketType.READLINK).putString(path)));
    }
    
    public void makeDir(String path, FileAttributes attrs) throws IOException
    {
        make(newRequest(PacketType.MKDIR) //
                .putString(path) //
                .putFileAttributes(attrs) //
        ).ensureStatusOK();
    }
    
    public void makeDir(String path) throws IOException
    {
        makeDir(path, new FileAttributes());
    }
    
    public void symlink(String linkpath, String targetpath) throws IOException
    {
        make(newRequest(PacketType.SYMLINK) //
                .putString(linkpath) //
                .putString(targetpath) //
        ).ensureStatusOK();
    }
    
    public void remove(String filename) throws IOException
    {
        make(newRequest(PacketType.REMOVE) //
                .putString(filename) //
        ).ensureStatusOK();
    }
    
    public void removeDir(String path) throws IOException
    {
        make(newRequest(PacketType.RMDIR) //
                .putString(path) //
        ).ensureStatus(StatusCode.OK);
    }
    
    private FileAttributes stat(PacketType pt, String path) throws IOException
    {
        return make(newRequest(pt).putString(path)) //
                .ensurePacketTypeIs(PacketType.ATTRS) //
                .readFileAttributes();
    }
    
    public FileAttributes stat(String path) throws IOException
    {
        return stat(PacketType.STAT, path);
    }
    
    public FileAttributes lstat(String path) throws IOException
    {
        return stat(PacketType.LSTAT, path);
    }
    
    public void rename(String oldPath, String newPath) throws IOException
    {
        make(newRequest(PacketType.RENAME) //
                .putString(oldPath) //
                .putString(newPath) //
        ).ensureStatusOK();
    }
    
    public String canonicalize(String path) throws IOException
    {
        return readSingleName(make(newRequest(PacketType.REALPATH).putString(path)));
    }
    
    private static String readSingleName(Response res) throws IOException
    {
        res.ensurePacketTypeIs(PacketType.NAME);
        if (res.readInt() == 1)
            return res.readString();
        else
            throw new SFTPException("Unexpected data in " + res.getType() + " packet");
    }
    
    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }
    
    public int getTimeout()
    {
        return timeout;
    }
    
}
