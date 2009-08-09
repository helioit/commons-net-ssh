package org.apache.commons.net.ssh.connection;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.IOUtils;

public class SessionChannel extends AbstractDirectChannel implements Session, Session.Command, Session.Shell,
        Session.Subsystem
{
    
    private Integer exitStatus;
    
    private Signal exitSignal;
    
    private Boolean flowControl;
    
    private final ChannelInputStream err = new ChannelInputStream(this, lwin);
    
    public SessionChannel(Connection conn)
    {
        super("session", conn);
    }
    
    public void allocateDefaultPTY() throws ConnectionException, TransportException
    {
        Map<PTYMode, Buffer> modes = new HashMap<PTYMode, Buffer>();
        modes.put(PTYMode.ISIG, new Buffer().putInt(1));
        modes.put(PTYMode.ICANON, new Buffer().putInt(1));
        modes.put(PTYMode.ECHO, new Buffer().putInt(0));
        modes.put(PTYMode.ECHOE, new Buffer().putInt(0));
        modes.put(PTYMode.ECHOK, new Buffer().putInt(0));
        modes.put(PTYMode.ECHONL, new Buffer().putInt(0));
        modes.put(PTYMode.NOFLSH, new Buffer().putInt(0));
        allocatePTY("vt100", 80, 40, 640, 480, modes);
    }
    
    public void allocatePTY(String term, int cols, int rows, int width, int height, Map<PTYMode, Buffer> modes)
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
        return flowControl;
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
        return getStreamAsString(in);
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
            flowControl = buf.getBoolean();
        else if ("exit-status".equals(req))
            exitStatus = buf.getInt();
        else if ("exit-signal".equals(req))
            exitSignal = Signal.fromString(buf.getString());
        else
            super.handleRequest(req, buf);
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
    
    public void startX11Forwarding(boolean singleConnection, String authProto, String authCookie, int screen,
            ConnectListener listener) throws ConnectionException, TransportException
    {
        sendChannelRequest("x11-req", true, //
                           new Buffer() //
                                       .putBoolean(singleConnection) //
                                       .putString(authProto) //
                                       .putString(authCookie) //
                                       .putInt(screen)).await(conn.getTimeout());
        new X11Forwarder(conn, listener);
    }
    
    public void waitForClose() throws ConnectionException
    {
        close.await(conn.getTimeout());
    }
    
    @Override
    protected void closeStreams()
    {
        super.closeStreams();
        IOUtils.closeQuietly(err);
    }
    
    @Override
    protected void gotEOF() throws TransportException
    {
        err.eof();
        super.gotEOF();
    }
    
    @Override
    protected void gotExtendedData(int dataTypeCode, Buffer buf) throws ConnectionException, TransportException
    {
        if (dataTypeCode == 1)
            doWrite(buf, err);
        else
            super.gotExtendedData(dataTypeCode, buf);
    }
    
}
