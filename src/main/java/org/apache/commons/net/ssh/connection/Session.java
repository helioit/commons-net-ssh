package org.apache.commons.net.ssh.connection;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.net.ssh.transport.TransportException;

public interface Session extends Channel
{
    
    interface Command extends Channel
    {
        
        String getErrorAsString() throws IOException;
        
        InputStream getErrorStream() throws IOException;
        
        Signal getExitSignal();
        
        Integer getExitStatus();
        
        String getOutputAsString() throws IOException;
        
        void signal(Signal sig) throws TransportException;
        
    }
    
    interface Shell extends Channel
    {
        
        Boolean canDoFlowControl();
        
        void changeWindowDimensions(int cols, int rows, int width, int height) throws TransportException;
        
        InputStream getErrorStream();
        
    }
    
    enum Signal
    {
        
        SIG_ABRT("ABRT"),
        SIG_ALRM("ALRM"),
        SIG_FPE("FPE"),
        SIG_HUP("HUP"),
        SIG_ILL("ILL"),
        SIG_INT("INT"),
        SIG_KILL("KILL"),
        SIG_PIPE("PIPE"),
        SIG_QUIT("QUIT"),
        SIG_SEGV("SEGV"),
        SIG_TERM("TERM"),
        SIG_USR1("USR1"),
        SIG_USR2("USR2"),
        UNKNOWN("");
        
        public static Signal fromString(String name)
        {
            for (Signal sig : Signal.values())
                if (sig.name.equals(name))
                    return sig;
            Signal unknown = UNKNOWN;
            unknown.name = name;
            return unknown;
        }
        
        private String name;
        
        private Signal(String name)
        {
            this.name = name;
        }
        
        public String getName()
        {
            return name;
        }
        
    }
    
    interface Subsystem extends Channel
    {
        // should this be here?
        Integer getExitStatus();
    }
    
    void allocateDefaultPTY() throws ConnectionException, TransportException;
    
    void allocatePTY(String term, int cols, int rows, int width, int height, Map<PTYMode, Integer> modes)
            throws ConnectionException, TransportException;
    
    void close() throws ConnectionException, TransportException;
    
    Command exec(String command) throws ConnectionException, TransportException;
    
    boolean isOpen();
    
    /* With OpenSSH default is to reject env vars: "AcceptEnv" config var shd be set * */
    void setEnvVar(String name, String value) throws ConnectionException, TransportException;
    
    Shell startShell() throws ConnectionException, TransportException;
    
    Subsystem startSubsysytem(String name) throws ConnectionException, TransportException;
    
    void startX11Forwarding(boolean singleConnection, String authProto, String authCookie, int screen,
            ConnectListener listener) throws ConnectionException, TransportException;
    
}
