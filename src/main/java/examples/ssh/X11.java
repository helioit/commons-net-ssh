package examples.ssh;

import java.net.InetSocketAddress;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.connection.Session;
import org.apache.commons.net.ssh.connection.ConnectListener.SocketForwardingConnectListener;
import org.apache.commons.net.ssh.connection.Session.Command;
import org.apache.commons.net.ssh.util.Pipe;

public class X11
{
    
    //    static {
    //        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("[%t] %p %c{2} %m%n")));
    //    }
    
    public static void main(String... args) throws Exception
    {
        SSHClient client = new SSHClient();
        client.initUserKnownHosts();
        
        client.connect("localhost");
        try {
            
            client.authPublickey(System.getProperty("user.name"));
            
            Session sess = client.startSession();
            
            /*
             * - The hex value is auth cookie from `xauth list`
             * 
             * - Forwarding incoming X connections to localhost:6000 only works if X is started
             * without the "-nolisten tcp" option (this is usually not the default for good reason)
             * 
             * There are some security concerns arising from both of the above points, but then this
             * snippet is intended to serve as a simple example...
             */
            sess.startX11Forwarding(false, "MIT-MAGIC-COOKIE-1", "85c0d66965c04b0b6ac3d2a21d6ced19", 0,
                                    new SocketForwardingConnectListener(new InetSocketAddress("localhost", 6000)));
            
            Command cmd = sess.exec("firefox");
            
            new Pipe("stdout", cmd.getInputStream(), System.out).start();
            new Pipe("stderr", cmd.getErrorStream(), System.err).start();
            
            // Wait for session & X11 channel to get closed
            client.getConnection().join();
            
        } finally {
            client.disconnect();
        }
    }
    
}