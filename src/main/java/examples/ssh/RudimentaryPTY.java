package examples.ssh;

import java.io.IOException;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.connection.Session;
import org.apache.commons.net.ssh.connection.Session.Shell;
import org.apache.commons.net.ssh.util.Pipe;

class RudimentaryPTY
{
    
    //    static {
    //        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d [%-15.15t] %-5p %-30.30c{1} - %m%n")));
    //    }
    
    public static void main(String... args) throws IOException
    {
        SSHClient client = new SSHClient();
        client.initUserKnownHosts();
        client.connect("localhost");
        Session session = null;
        try {
            
            client.authPublickey(System.getProperty("user.name"));
            
            session = client.startSession();
            session.allocateDefaultPTY();
            Shell shell = session.startShell();
            
            new Pipe("stdout", shell.getInputStream(), System.out) //
                                                                  .bufSize(session.getLocalMaxPacketSize()) //
                                                                  .start();
            
            new Pipe("stderr", shell.getErrorStream(), System.err) //
                                                                  .bufSize(session.getLocalMaxPacketSize()) //
                                                                  .start();
            
            // Now make System.in act as stdin. To exit, hit Ctrl+D (since that results in an EOF on System.in)
            
            // This is kinda messy because java only allows console input after you hit return
            // But this is just an example... a GUI app could implement a proper PTY
            
            Pipe.pipe(System.in, shell.getOutputStream(), session.getRemoteMaxPacketSize(), true);
            
        } finally {
            if (session != null)
                session.close();
            client.disconnect();
        }
    }
    
}
