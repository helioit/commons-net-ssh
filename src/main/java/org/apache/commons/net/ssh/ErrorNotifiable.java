package org.apache.commons.net.ssh;

public interface ErrorNotifiable
{
    
    class Util
    {
        public static void alertAll(SSHException error, ErrorNotifiable... notifiables)
        {
            System.err.println("here");
            for (ErrorNotifiable notifiable : notifiables) {
                System.err.println("Notifying " + notifiable);
                notifiable.notifyError(error);
            }
        }
        
        public static void something(SSHException error)
        {
            System.err.println(error);
        }
    }
    
    void notifyError(SSHException error);
    
}
