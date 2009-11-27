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

public class SFTP
{
    
    /** Logger */
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final int PROTOCOL_VERSION = 3;
    
    public int timeout = 60;
    
    private final Subsystem sub;
    private final PacketReader reader;
    private final OutputStream out;
    
    private long reqID;
    
    private int negotiatedVersion;
    
    private final Map<String, String> serverExtensions = new HashMap<String, String>();
    
    public SFTP(SessionFactory ssh) throws ConnectionException, TransportException
    {
        sub = ssh.startSession().startSubsystem("sftp");
        out = sub.getOutputStream();
        reader = new PacketReader(sub.getInputStream());
    }
    
    public Subsystem getSubsystem()
    {
        return sub;
    }
    
    public SFTP init() throws IOException
    {
        Packet pk = new Packet();
        pk.putByte(PacketType.INIT.toByte());
        pk.putInt(PROTOCOL_VERSION);
        transmit(pk);
        
        Packet response = reader.readPacket();
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
    
    private void transmit(Packet payload) throws IOException
    {
        final int len = payload.available();
        out.write((byte) (len >> 24));
        out.write((byte) (len >> 16));
        out.write((byte) (len >> 8));
        out.write((byte) (len));
        out.write(payload.array(), 0, len);
        out.flush();
    }
    
    public void send(Request req) throws IOException
    {
        reader.expectResponseTo(req);
        log.debug("Sending {}", req);
        transmit(req);
    }
    
    public RemoteFile open(String path, Set<OpenMode> modes, FileAttributes fa) throws IOException
    {
        Request req = newRequest(PacketType.OPEN);
        req.putString(path);
        req.putInt(OpenMode.toMask(modes));
        req.putFileAttributes(fa);
        
        send(req);
        
        Response res = req.getFuture().get(timeout);
        res.ensurePacket(PacketType.HANDLE);
        return new RemoteFile(this, path, res.readString());
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
        Request req = newRequest(PacketType.OPENDIR);
        req.putString(path);
        
        send(req);
        
        Response res = req.getFuture().get(timeout);
        res.ensurePacket(PacketType.HANDLE);
        return new RemoteDir(this, path, res.readString());
    }
    
    public void setAttributes(String path, FileAttributes attrs) throws IOException
    {
        Request req = newRequest(PacketType.SETSTAT);
        req.putString(path);
        req.putFileAttributes(attrs);
        send(req);
        Response res = req.getFuture().get(timeout);
        res.ensureStatusOK();
    }
    
    public String readLink(String path) throws IOException
    {
        Request req = newRequest(PacketType.READLINK);
        req.putString(path);
        send(req);
        return readSingleName(req.getFuture().get(timeout));
    }
    
    public void makeDir(String path, FileAttributes attrs) throws IOException
    {
        Request req = newRequest(PacketType.MKDIR);
        req.putString(path);
        req.putFileAttributes(attrs);
        send(req);
        req.getFuture().get(timeout).ensureStatusOK();
    }
    
    public void makeDir(String path) throws IOException
    {
        makeDir(path, new FileAttributes());
    }
    
    public void symlink(String linkpath, String targetpath) throws IOException
    {
        Request req = newRequest(PacketType.SYMLINK);
        req.putString(linkpath).putString(targetpath);
        send(req);
        req.getFuture().get(timeout).ensureStatusOK();
    }
    
    public void remove(String filename) throws IOException
    {
        Request req = newRequest(PacketType.REMOVE);
        req.putString(filename);
        send(req);
        req.getFuture().get(timeout).ensureStatusOK();
    }
    
    public void removeDir(String path) throws IOException
    {
        Request req = newRequest(PacketType.RMDIR);
        req.putString(path);
        send(req);
        req.getFuture().get(timeout).ensureStatus(StatusCode.OK);
    }
    
    private FileAttributes stat(PacketType pt, String path) throws IOException
    {
        Request req = newRequest(pt);
        req.putString(path);
        send(req);
        Response res = req.getFuture().get(timeout);
        res.ensurePacket(PacketType.ATTRS);
        return res.readFileAttributes();
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
        Request req = newRequest(PacketType.RENAME);
        req.putString(oldPath).putString(newPath);
        send(req);
        req.getFuture().get(timeout).ensureStatusOK();
    }
    
    public String canonicalize(String path) throws IOException
    {
        Request req = newRequest(PacketType.REALPATH);
        req.putString(path);
        send(req);
        return readSingleName(req.getFuture().get(timeout));
    }
    
    private static String readSingleName(Response res) throws IOException
    {
        res.ensurePacket(PacketType.NAME);
        if (res.readInt() == 1)
            return res.readString();
        else
            throw new SFTPException("Unexpected data in " + res.getType() + " packet");
    }
    
}
