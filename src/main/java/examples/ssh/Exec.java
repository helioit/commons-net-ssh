package examples.ssh;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.connection.Session.Command;

/**
 * This examples demonstrates how a remote command can be executed.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class Exec
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
            
            Command cmd = ssh.startSession().exec("man sshd_config");
            
            //Pipe.pipe(cmd.getInputStream(), System.out, cmd.getLocalMaxPacketSize(), false);
            System.out.print(cmd.getOutputAsString());
            System.out.println("\n** exit status: " + cmd.getExitStatus());
            
        } finally {
            ssh.disconnect();
        }
    }
    
}
