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
package org.apache.commons.net.ssh.connection;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;

/**
 * A {@code session} channel provides execution of a remote {@link Command command}, {@link Shell
 * shell} or {@link Subsystem subsystem}. Before this requests like starting X11 forwarding, setting
 * environment variables, window dimensions etc. can be made.
 * <p>
 * It is not legal to reuse a {@code session} channel for more than one of command, shell, or
 * subsystem. Once one of these has been started, this instance's API is invalid and that of the
 * {@link Command specific} {@link Shell targets} {@link Subsystem returned} should be used.
 * 
 * @see Command
 * @see Shell
 * @see Subsystem
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public interface Session extends Channel
{
    
    /**
     * Remote command.
     */
    interface Command extends Channel
    {
        
        /**
         * Read from the command's stderr stream into a string (blocking).
         * 
         * @return the stderr output as a string
         * @throws IOException
         *             if error reading from the stream
         */
        String getErrorAsString() throws IOException;
        
        /**
         * Returns the command's stderr stream.
         */
        InputStream getErrorStream();
        
        /**
         * Returns the {@link Signal signal} the command exit with, or {@code null} if this
         * information was not received.
         */
        Signal getExitSignal();
        
        /**
         * Returns the exit status of the command if it was received, or {@code null} if this
         * information was not received.
         */
        Integer getExitStatus();
        
        /**
         * Read from the command's stdout stream into a string (blocking).
         * 
         * @return the command's output as a string
         * @throws IOException
         *             if error reading from the stream
         */
        String getOutputAsString() throws IOException;
        
        /**
         * Send a signal to the remote command.
         * 
         * @param sig
         *            {@link Signal} identifier
         * @throws TransportException
         *             if error sending the signal
         */
        void signal(Signal sig) throws TransportException;
        
    }
    
    /**
     * Shell.
     */
    interface Shell extends Channel
    {
        
        Boolean canDoFlowControl();
        
        void changeWindowDimensions(int cols, int rows, int width, int height) throws TransportException;
        
        InputStream getErrorStream();
        
        void signal(Signal sig) throws TransportException;
        
    }
    
    /**
     * The different signals that may be sent or received.
     */
    public enum Signal
    {
        
        ABRT("ABRT"),
        ALRM("ALRM"),
        FPE("FPE"),
        HUP("HUP"),
        ILL("ILL"),
        INT("INT"),
        KILL("KILL"),
        PIPE("PIPE"),
        QUIT("QUIT"),
        SEGV("SEGV"),
        TERM("TERM"),
        USR1("USR1"),
        USR2("USR2"),
        UNKNOWN("UNKNOWN");
        
        public static Signal fromString(String name)
        {
            for (Signal sig : Signal.values())
                if (sig.name.equals(name))
                    return sig;
            return UNKNOWN;
        }
        
        private final String name;
        
        private Signal(String name)
        {
            this.name = name;
        }
        
        @Override
        public String toString()
        {
            return name;
        }
        
    }
    
    interface Subsystem extends Channel
    { // there isn't really any subsystem-specific API, or is there...
    }
    
    void allocateDefaultPTY() throws ConnectionException, TransportException;
    
    void allocatePTY(String term, int cols, int rows, int width, int height, Map<PTYMode, Buffer> modes)
            throws ConnectionException, TransportException;
    
    /**
     * Execute a remote command.
     * 
     * @param command
     * @return {@link Command} instance which should now be used
     * @throws ConnectionException
     *             if the request to execute the command failed
     * @throws TransportException
     *             if there is an error sending the request
     */
    Command exec(String command) throws ConnectionException, TransportException;
    
    /**
     * Request X11 forwarding.
     * 
     * @param authProto
     *            X11 authentication protocol name
     * @param authCookie
     *            X11 authentication cookie
     * @param screen
     *            X11 screen number
     * @throws ConnectionException
     *             if the request fails
     * @throws TransportException
     *             if there is an error sending the request
     */
    void reqX11Forwarding(String authProto, String authCookie, int screen) throws ConnectionException,
            TransportException;
    
    /**
     * Set an enviornment variable.
     * 
     * @param name
     *            name of the variable
     * @param value
     *            value to set
     * @throws ConnectionException
     *             if the request fails
     * @throws TransportException
     *             error writing the request
     */
    void setEnvVar(String name, String value) throws ConnectionException, TransportException;
    
    /**
     * Request a shell.
     * 
     * @return {@link Shell} instance which should now be used
     * @throws ConnectionException
     *             if the request fails
     * @throws TransportException
     *             if there is an error sending the request
     */
    Shell startShell() throws ConnectionException, TransportException;
    
    /**
     * Request a subsystem.
     * 
     * @param name
     * @return {@link Subsystem} instance which should now be used
     * @throws ConnectionException
     *             if the request fails
     * @throws TransportException
     *             if there is an error sending the request
     */
    Subsystem startSubsysytem(String name) throws ConnectionException, TransportException;
    
}
