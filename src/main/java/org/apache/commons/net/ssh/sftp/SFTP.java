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
import java.util.Map.Entry;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.connection.ConnectionException;
import org.apache.commons.net.ssh.connection.Session.Subsystem;
import org.apache.commons.net.ssh.sftp.Response.StatusCode;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
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
    
    public SFTP(SSHClient ssh) throws ConnectionException, TransportException
    {
        sub = ssh.startSession().startSubsystem("sftp");
        out = sub.getOutputStream();
        reader = new PacketReader(sub.getInputStream());
    }
    
    public void init() throws IOException
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
        
        for (Entry<String, String> ext : serverExtensions.entrySet())
            System.out.println(ext.getKey() + ": " + ext.getValue());
        
        // Start reader thread
        reader.start();
    }
    
    public void send(Request req) throws IOException
    {
        reader.expectResponseTo(req);
        transmit(req);
    }
    
    public RemoteFile open(String filename, Set<FileMode> modes, FileAttributes fa) throws IOException
    {
        Request req = newRequest(PacketType.OPEN);
        req.putString(filename);
        req.putInt(FileMode.toMask(modes));
        req.putFileAttributes(fa);
        
        send(req);
        
        Response response = req.getFuture().get(timeout);
        response.ensureStatus(StatusCode.OK);
        // got here => can get the file handle
        return new RemoteFile(this, response.readString());
    }
    
    public RemoteFile open(String filename, Set<FileMode> modes) throws IOException
    {
        return open(filename, modes, new FileAttributes.Builder().build());
    }
    
    public RemoteFile open(String filename) throws IOException
    {
        return open(filename, EnumSet.of(FileMode.READ));
    }
    
    public RemoteDir openDir(String path) throws IOException
    {
        Request req = newRequest(PacketType.OPENDIR);
        req.putString(path);
        
        send(req);
        
        Response response = req.getFuture().get(timeout);
        response.ensureStatus(StatusCode.OK);
        return new RemoteDir(this, response.readString());
    }
    
    public int getOperativeProtocolVersion()
    {
        return negotiatedVersion;
    }
    
    public synchronized Request newRequest(PacketType type)
    {
        return new Request(type, reqID + 1 & 0xffffffffL);
    }
    
    public void transmit(Packet payload) throws IOException
    {
        final int len = payload.available();
        out.write((byte) (len << 24 & 0xff000000));
        out.write((byte) (len << 16 & 0x00ff0000));
        out.write((byte) (len << 8 & 0x0000ff00));
        out.write((byte) (len & 0x000000ff));
        out.write(payload.array(), 0, len);
        out.flush();
    }
    
    // everything below this line is temporary :)
    
    static
    {
        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d [%-15.15t] %-5p %-30.30c{1} - %m%n")));
    }
    
    public static void main(String[] args) throws IOException
    {
        SSHClient ssh = new SSHClient();
        ssh.loadKnownHosts();
        
        ssh.connect("localhost");
        try
        {
            
            ssh.authPublickey(System.getProperty("user.name"));
            
            SFTP sftp = new SFTP(ssh);
            sftp.init();
            
        } finally
        {
            ssh.disconnect();
        }
    }
    
}
