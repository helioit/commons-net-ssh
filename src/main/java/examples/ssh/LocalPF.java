package examples.ssh;

import java.net.InetSocketAddress;

import org.apache.commons.net.ssh.SSHClient;

/**
 * This example demonstrates local port forwarding, i.e. when we listen on a particular address and
 * port; and forward all incoming connections to SSH server which furthter forwards them to a
 * specified address and port.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class LocalPF
{
    
    //    static {
    //        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d [%-15.15t] %-5p %-30.30c{1} - %m%n")));
    //    }
    
    public static void main(String... args) throws Exception
    {
        SSHClient ssh = new SSHClient();
        
        ssh.loadKnownHosts();
        
        ssh.connect("localhost");
        try {
            
            ssh.authPublickey(System.getProperty("user.name"));
            
            /*
             * _We_ listen on localhost:8080 and forward all connections on to server, which then
             * forwards it to google.com:80
             */
            ssh.newLocalPortForwarder(new InetSocketAddress("localhost", 8080), "google.com", 80).startListening();
            
            // Something to hang on to
            ssh.getTransport().join();
            
        } finally {
            ssh.disconnect();
        }
    }
    
}
