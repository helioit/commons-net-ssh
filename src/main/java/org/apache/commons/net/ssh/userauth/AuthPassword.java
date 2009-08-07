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
package org.apache.commons.net.ssh.userauth;

import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.PasswordFinder;
import org.apache.commons.net.ssh.util.Constants.Message;
import org.apache.commons.net.ssh.util.PasswordFinder.Resource;

public class AuthPassword extends AbstractAuthMethod
{
    
    private final PasswordFinder pwdf;
    
    public AuthPassword(PasswordFinder pwdf)
    {
        super("password");
        this.pwdf = pwdf;
    }
    
    @Override
    public Buffer buildReq() throws UserAuthException
    {
        Resource resource = getResource();
        log.info("Requesting password for " + resource);
        char[] password = pwdf.reqPassword(resource);
        try {
            if (password == null)
                throw new UserAuthException("Was given null password for " + resource);
            else
                return super.buildReq() // the generic stuff
                            .putBoolean(false) // no, we are not responding to a CHANGEREQ
                            .putPassword(password);
        } finally {
            password = null;
        }
    }
    
    @Override
    public void handle(Message cmd, Buffer buf) throws UserAuthException, TransportException
    {
        if (cmd == Message.USERAUTH_60)
            throw new UserAuthException("Password change request received; unsupported operation");
        else
            super.handle(cmd, buf);
    }
    
    @Override
    public boolean shouldRetry()
    {
        return pwdf.shouldRetry(getResource());
    }
    
    protected Resource getResource()
    {
        return new Resource(Resource.Type.ACCOUNT, params.getUsername() + "@"
                + params.getTransport().getRemoteHost().getHostName());
    }
    
}
