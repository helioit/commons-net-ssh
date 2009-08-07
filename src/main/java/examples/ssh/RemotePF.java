package examples.ssh;

import java.net.InetSocketAddress;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.connection.ConnectListener.SocketForwardingConnectListener;
import org.apache.commons.net.ssh.connection.RemotePortForwarder.Forward;

public class RemotePF
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
            
            /*
             * We make _server_ listen on port 8080, which forwards all connections to us as a
             * channel, and we further forward all such channels to google.com:80
             */
            client.getRemotePortForwarder()
                  .bind(new Forward("127.0.0.1", 8080), //
                        new SocketForwardingConnectListener(new InetSocketAddress("google.com", 80)));
            
            // something to hang on to so forwarding stays
            client.getTransport().join();
            
        } finally {
            client.disconnect();
        }
    }
    
}
