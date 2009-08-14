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

/**
 * Controls how much data we can send before an adjustment notification from remote end is required.
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class RemoteWindow extends Window
{
    
    public RemoteWindow(Channel chan)
    {
        super(chan, false);
    }
    
    public synchronized void waitAndConsume(int howMuch) throws ConnectionException
    {
        while (size < howMuch) {
            log.debug("Waiting, need window space for {} bytes", howMuch);
            try {
                wait();
            } catch (InterruptedException ie) {
                throw new ConnectionException(ie);
            }
        }
        consume(howMuch);
    }
    
}
