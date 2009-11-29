/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package examples.ssh;

import java.io.IOException;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.connection.Session;
import org.apache.commons.net.ssh.connection.Session.Shell;
import org.apache.commons.net.ssh.util.Pipe;

/**
 * A very rudimentary psuedo-terminal based on console I/O.
 */
class RudimentaryPTY
{
    
    // static {
    // BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%d [%-15.15t] %-5p %-30.30c{1} - %m%n")));
    // }
    
    public static void main(String... args) throws IOException
    {
        SSHClient ssh = new SSHClient();
        
        ssh.loadKnownHosts();
        
        ssh.connect("localhost");
        
        Shell shell = null;
        
        try
        {
            
            ssh.authPublickey(System.getProperty("user.name"));
            
            Session session = ssh.startSession();
            session.allocateDefaultPTY();
            
            shell = session.startShell();
            
            new Pipe("stdout", shell.getInputStream(), System.out) //
                    .bufSize(shell.getLocalMaxPacketSize()) //
                    .start();
            
            new Pipe("stderr", shell.getErrorStream(), System.err) //
                    .bufSize(shell.getLocalMaxPacketSize()) //
                    .start();
            
            // Now make System.in act as stdin. To exit, hit Ctrl+D (since that results in an EOF on System.in)
            
            // This is kinda messy because java only allows console input after you hit return
            // But this is just an example... a GUI app could implement a proper PTY
            
            Pipe.pipe(System.in, shell.getOutputStream(), shell.getRemoteMaxPacketSize(), false);
            
        } finally
        {
            
            if (shell != null)
                shell.close();
            
            ssh.disconnect();
        }
    }
    
}
