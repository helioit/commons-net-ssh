package examples.ssh;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.scp.SCPUploadClient;

/**
 * This example demonstrates uploading of a file over SCP to the SSH server.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class SCPUpload
{
    
    //    static {
    //        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d [%-15.15t] %-5p %-30.30c{1} - %m%n")));
    //    }
    
    public static void main(String[] args) throws Exception
    {
        SSHClient ssh = new SSHClient();
        ssh.loadKnownHosts();
        ssh.connect("localhost");
        try {
            ssh.authPublickey(System.getProperty("user.name"));
            
            // Compression = significant speedup for large file transfers on fast links
            // present here to demo renegotiation - could have just put this before connect()             
            ssh.useCompression();
            
            new SCPUploadClient(ssh).copy("/tmp/ten", ".");
            
        } finally {
            ssh.disconnect();
        }
    }
    
}