package examples.ssh;

import java.net.InetSocketAddress;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.connection.Session;
import org.apache.commons.net.ssh.connection.SocketForwardingConnectListener;
import org.apache.commons.net.ssh.connection.Session.Command;
import org.apache.commons.net.ssh.util.Pipe;

public class X11
{
    
    //    static {
    //        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d [%-15.15t] %-5p %-30.30c{1} - %m%n")));
    //    }
    
    public static void main(String... args) throws Exception
    {
        SSHClient ssh = new SSHClient();
        
        ssh.initUserKnownHosts();
        
        /*
         * NOTE: Forwarding incoming X connections to localhost:6000 only works if X is started
         * without the "-nolisten tcp" option (this is usually not the default for good reason)
         */
        ssh.registerX11Forwarder(new SocketForwardingConnectListener(new InetSocketAddress("localhost", 6000)));
        
        ssh.connect("localhost");
        try {
            
            ssh.authPublickey(System.getProperty("user.name"));
            
            Session sess = ssh.startSession();
            
            /*
             * It is recommendable to send a fake cookie, and in your ConnectListener when a
             * connection comes in replace it with the real one. But here simply one from `xauth
             * list` is being used.
             */
            sess.reqX11Forwarding("MIT-MAGIC-COOKIE-1", "499964c789ea9bc82e117c1e696cbf88", 0);
            
            Command cmd = sess.exec("firefox");
            
            new Pipe("stdout", cmd.getInputStream(), System.out).start();
            new Pipe("stderr", cmd.getErrorStream(), System.err).start();
            
            // Wait for session & X11 channel to get closed
            ssh.getConnection().join();
            
        } finally {
            ssh.disconnect();
        }
    }
}