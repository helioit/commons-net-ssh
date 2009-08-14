package examples.ssh;

import java.net.InetSocketAddress;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

public class LocalPF
{
    
    static {
        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d [%-15.15t] %-5p %-30.30c{1} - %m%n")));
    }
    
    public static void main(String... args) throws Exception
    {
        SSHClient client = new SSHClient();
        
        client.initUserKnownHosts();
        
        client.connect("localhost");
        try {
            
            client.authPublickey(System.getProperty("user.name"));
            
            /*
             * _We_ listen on localhost:8080 and forward all connections on to server, which then
             * forwards it to google.com:80
             */
            client.newLocalPortForwarder(new InetSocketAddress("localhost", 8080), "google.com", 80).startListening();
            
            // Something to hang on to
            client.getTransport().join();
            
        } finally {
            client.disconnect();
        }
    }
    
}
