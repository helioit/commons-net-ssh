package examples.ssh;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.scp.SCPUploadClient;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

public class SCPUpload
{
    
    static {
        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("[%t] %p %c{2} %m%n")));
    }
    
    public static void main(String[] args) throws Exception
    {
        SSHClient client = new SSHClient();
        client.initUserKnownHosts();
        client.connect("localhost");
        try {
            client.authPublickey(System.getProperty("user.name"));
            new SCPUploadClient(client).copy("/home/shikhar/logs", "/tmp/");
        } finally {
            client.disconnect();
        }
    }
    
}