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
package org.apache.commons.net.ssh.connection;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.IOUtils;

/**
 * {@link Session} implementation.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class SessionChannel extends AbstractDirectChannel implements Session, Session.Command, Session.Shell,
        Session.Subsystem
{
    
    private Integer exitStatus;
    
    private Signal exitSignal;
    private Boolean wasCoreDumped;
    private String exitErrMsg;
    
    private Boolean canDoFlowControl;
    
    private final ChannelInputStream err = new ChannelInputStream(this, lwin);
    
    public SessionChannel(Connection conn)
    {
        super("session", conn);
    }
    
    public void allocateDefaultPTY() throws ConnectionException, TransportException
    {
        /*
         * FIXME (maybe?): These modes were originally copied from what SSHD was doing; and then the
         * echo modes were set to 0 to better serve the PTY example. Not sure what default PTY modes
         * should be.
         */
        Map<PTYMode, Integer> modes = new HashMap<PTYMode, Integer>();
        modes.put(PTYMode.ISIG, 1);
        modes.put(PTYMode.ICANON, 1);
        modes.put(PTYMode.ECHO, 0);
        modes.put(PTYMode.ECHOE, 0);
        modes.put(PTYMode.ECHOK, 0);
        modes.put(PTYMode.ECHONL, 0);
        modes.put(PTYMode.NOFLSH, 0);
        allocatePTY("vt100", 0, 0, 0, 0, modes);
    }
    
    public void allocatePTY(String term, int cols, int rows, int width, int height, Map<PTYMode, Integer> modes)
            throws ConnectionException, TransportException
    {
        sendChannelRequest("pty-req", //
                           true, // 
                           new Buffer().putString(term) //
                                       .putInt(cols) //
                                       .putInt(rows) //
                                       .putInt(width) //
                                       .putInt(height) //
                                       .putBytes(PTYMode.encode(modes)) //
        ).await(conn.getTimeout()); // wait for reply
    }
    
    public Boolean canDoFlowControl()
    {
        return canDoFlowControl;
    }
    
    public void changeWindowDimensions(int cols, int rows, int width, int height) throws TransportException
    {
        sendChannelRequest("pty-req", //
                           false, //
                           new Buffer().putInt(cols) //
                                       .putInt(rows) //
                                       .putInt(width) //
                                       .putInt(height));
    }
    
    public Command exec(String command) throws ConnectionException, TransportException
    {
        log.info("Will request to exec `{}`", command);
        sendChannelRequest("exec", true, new Buffer().putString(command)).await(conn.getTimeout());
        return this;
    }
    
    public String getErrorAsString() throws IOException
    {
        return getStreamAsString(err);
    }
    
    public InputStream getErrorStream()
    {
        return err;
    }
    
    public String getExitErrorMessage()
    {
        return exitErrMsg;
    }
    
    public Signal getExitSignal()
    {
        return exitSignal;
    }
    
    public Integer getExitStatus()
    {
        return exitStatus;
    }
    
    public String getOutputAsString() throws IOException
    {
        return getStreamAsString(getInputStream());
    }
    
    public String getStreamAsString(InputStream stream) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        int r;
        while ((r = stream.read()) != -1)
            sb.append((char) r);
        return sb.toString();
    }
    
    @Override
    public void handleRequest(String req, Buffer buf) throws ConnectionException, TransportException
    {
        if ("xon-xoff".equals(req))
            canDoFlowControl = buf.getBoolean();
        else if ("exit-status".equals(req))
            exitStatus = buf.getInt();
        else if ("exit-signal".equals(req)) {
            exitSignal = Signal.fromString(buf.getString());
            wasCoreDumped = buf.getBoolean(); // core dumped
            exitErrMsg = buf.getString();
            sendClose();
        } else
            super.handleRequest(req, buf);
    }
    
    public void reqX11Forwarding(String authProto, String authCookie, int screen) throws ConnectionException,
            TransportException
    {
        sendChannelRequest("x11-req", true, //
                           new Buffer() //
                                       .putBoolean(false).putString(authProto) //
                                       .putString(authCookie) //
                                       .putInt(screen)).await(conn.getTimeout());
    }
    
    public void setEnvVar(String name, String value) throws ConnectionException, TransportException
    {
        sendChannelRequest("env", true, new Buffer().putString(name).putString(value)).await(conn.getTimeout());
    }
    
    public void signal(Signal sig) throws TransportException
    {
        sendChannelRequest("signal", false, new Buffer().putString(sig.toString()));
    }
    
    public Shell startShell() throws ConnectionException, TransportException
    {
        sendChannelRequest("shell", true, null).await(conn.getTimeout());
        return this;
    }
    
    public Subsystem startSubsysytem(String name) throws ConnectionException, TransportException
    {
        log.info("Will request `{}` subsystem", name);
        sendChannelRequest("subsystem", true, new Buffer().putString(name)).await(conn.getTimeout());
        return this;
    }
    
    public Boolean getExitWasCoreDumped()
    {
        return wasCoreDumped;
    }
    
    @Override
    protected void closeAllStreams()
    {
        super.closeAllStreams();
        IOUtils.closeQuietly(err);
    }
    
    @Override
    protected void eofInputStreams()
    {
        err.eof(); // also close the stderr stream
        super.eofInputStreams();
    }
    
    @Override
    protected void gotExtendedData(int dataTypeCode, Buffer buf) throws ConnectionException, TransportException
    {
        if (dataTypeCode == 1)
            receiveInto(buf, err);
        else
            super.gotExtendedData(dataTypeCode, buf);
    }
    
}
