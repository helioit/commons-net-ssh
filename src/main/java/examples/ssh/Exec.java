package examples.ssh;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.connection.Session.Command;

public class Exec
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
            
            Command cmd = client.startSession().exec("uptime");
            
            System.out.print(cmd.getOutputAsString());
            System.out.println("\n** exit status: " + cmd.getExitStatus());
            
        } finally {
            client.disconnect();
        }
    }
    
}
