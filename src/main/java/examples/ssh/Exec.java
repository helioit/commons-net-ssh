package examples.ssh;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.connection.Session.Command;
import org.apache.commons.net.ssh.util.Pipe;

public class Exec
{
    
    //    static {
    //        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d [%-15.15t] %-5p %-30.30c{1} - %m%n")));
    //    }
    
    public static void main(String... args) throws Exception
    {
        SSHClient client = new SSHClient();
        client.initUserKnownHosts();
        
        client.connect("localhost");
        try {
            
            client.authPublickey(System.getProperty("user.name"));
            
            Command cmd = client.startSession().exec("uptime");
            
            Pipe.pipe(cmd.getInputStream(), System.out, cmd.getLocalMaxPacketSize(), false);
            //System.out.print(cmd.getOutputAsString());
            System.out.println("\n** exit status: " + cmd.getExitStatus());
            
        } finally {
            client.disconnect();
        }
    }
    
}
