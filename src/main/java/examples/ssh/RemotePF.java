package examples.ssh;

import java.net.InetSocketAddress;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.connection.SocketForwardingConnectListener;
import org.apache.commons.net.ssh.connection.RemotePortForwarder.Forward;

/**
 * This example demonstrates remote port forwarding i.e. when the remote host is made to listen on a
 * specific address and port; and forwards us incoming connections.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class RemotePF
{
    
    //    static {
    //        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d [%-15.15t] %-5p %-30.30c{1} - %m%n")));
    //    }
    
    public static void main(String... args) throws Exception
    {
        SSHClient client = new SSHClient();
        client.loadKnownHosts();
        
        client.connect("localhost");
        try {
            
            client.authPublickey(System.getProperty("user.name"));
            
            /*
             * We make _server_ listen on port 8080, which forwards all connections to us as a
             * channel, and we further forward all such channels to google.com:80
             */
            client.getRemotePortForwarder()
                  .bind(new Forward(8080), //
                        new SocketForwardingConnectListener(new InetSocketAddress("google.com", 80)));
            
            // Something to hang on to so forwarding stays
            client.getTransport().join();
            
        } finally {
            client.disconnect();
        }
    }
    
}
