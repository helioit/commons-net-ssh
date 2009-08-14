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

import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;

/**
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public interface ForwardedChannelOpener
{
    
    /**
     * Returns the name of the channel type this opener can handle.
     */
    String getChannelType();
    
    /**
     * {@code CHANNEL_OPEN} requests for the channel type claimed by this opener are delegated to
     * it.
     * 
     * @param buf
     *            {@link Buffer} containg the request except for the message identifier and channel
     *            type field
     */
    void handleOpen(Buffer buf) throws ConnectionException, TransportException;
    
}
