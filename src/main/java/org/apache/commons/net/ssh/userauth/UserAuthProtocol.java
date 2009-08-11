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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.net.ssh.AbstractService;
import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.Service;
import org.apache.commons.net.ssh.transport.Transport;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Event;
import org.apache.commons.net.ssh.util.Constants.DisconnectReason;
import org.apache.commons.net.ssh.util.Constants.Message;

/**
 * {@link UserAuth} implementation.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class UserAuthProtocol extends AbstractService implements UserAuth, AuthParams
{
    
    protected final ReentrantLock lock = new ReentrantLock();
    protected final Event<UserAuthException> result =
            new Event<UserAuthException>("userauth result", UserAuthException.chainer, lock);
    protected final Deque<UserAuthException> savedEx = new ArrayDeque<UserAuthException>();
    protected final Set<String> allowed = new HashSet<String>();
    
    protected AuthMethod currentMethod = new AuthNone(); // currently active method
    protected String banner; // auth banner
    
    protected boolean partialSuccess;
    
    protected String username;
    protected Service nextService;
    
    public UserAuthProtocol(Transport trans)
    {
        super("ssh-userauth", trans);
    }
    
    /**
     * 
     */
    public synchronized void authenticate(String username, Service nextService, Iterable<AuthMethod> methods)
            throws UserAuthException, TransportException
    {
        this.username = username;
        this.nextService = nextService;
        
        request(); // Request "ssh-userauth" service
        
        allowed.add(currentMethod.getName()); // "none" auth
        
        Iterator<AuthMethod> iter = methods.iterator();
        
        for (;;) {
            
            if (!allowed.contains(currentMethod.getName())) {
                save(currentMethod.getName() + " auth not allowed by server");
                break;
            } else
                log.info("Trying `{}` auth...", currentMethod.getName());
            
            boolean success = false;
            try {
                success = tryWith(currentMethod);
            } catch (UserAuthException e) { // Give other methods a shot
                save(e);
            }
            
            if (success) {
                log.info("`{}` auth successful", currentMethod.getName());
                return;
            } else {
                log.info("`{}` auth failed", currentMethod.getName());
                if (iter.hasNext())
                    currentMethod = iter.next();
                else {
                    currentMethod = null;
                    break;
                }
            }
            
        }
        
        log.debug("Had {} saved exception(s)", savedEx.size());
        throw new UserAuthException("Exhausted available authentication methods", savedEx.peek());
    }
    
    public String getBanner()
    {
        return banner;
    }
    
    public String getNextServiceName()
    {
        return nextService.getName();
    }
    
    /**
     * Returns the exceptions that occured during authentication process but were ignored because
     * more methods were available for trying.
     * 
     * @return deque of saved exceptions
     */
    public Deque<UserAuthException> getSavedExceptions()
    {
        return savedEx;
    }
    
    public String getUsername()
    {
        return username;
    }
    
    public boolean hadPartialSuccess()
    {
        return partialSuccess;
    }
    
    @Override
    public void handle(Message msg, Buffer buf) throws SSHException
    {
        if (!msg.in(50, 80)) // ssh-userauth packets have message numbers between 50-80
            throw new TransportException(DisconnectReason.PROTOCOL_ERROR);
        
        switch (msg)
        {
        case USERAUTH_BANNER:
            gotBanner(buf);
            break;
        case USERAUTH_SUCCESS:
            gotSuccess();
            break;
        case USERAUTH_FAILURE:
            gotFailure(buf);
            break;
        default:
            gotUnknown(msg, buf);
        }
    }
    
    @Override
    public void notifyError(SSHException error)
    {
        super.notifyError(error);
        result.error(error);
    }
    
    protected void gotBanner(Buffer buf)
    {
        banner = buf.getString();
    }
    
    protected void gotFailure(Buffer buf) throws UserAuthException, TransportException
    {
        allowed.clear();
        allowed.addAll(Arrays.<String> asList(buf.getString().split(",")));
        partialSuccess |= buf.getBoolean();
        if (allowed.contains(currentMethod.getName()) && currentMethod.shouldRetry())
            currentMethod.request();
        else {
            save(currentMethod.getName() + " auth failed");
            result.set(false);
        }
    }
    
    protected void gotSuccess()
    {
        trans.setAuthenticated(); // So it can put delayed compression into force if applicable
        trans.setService(nextService); // We aren't in charge anymore, next service is
        result.set(true);
    }
    
    protected void gotUnknown(Message msg, Buffer buf) throws SSHException
    {
        if (currentMethod == null || result == null)
            trans.sendUnimplemented();
        
        log.debug("Asking {} method to handle {} packet", currentMethod.getName(), msg);
        try {
            currentMethod.handle(msg, buf);
        } catch (UserAuthException e) {
            result.error(e);
        }
    }
    
    protected void save(String msg)
    {
        save(new UserAuthException(msg));
    }
    
    protected void save(UserAuthException e)
    {
        log.error("Saving for later - {}", e.toString());
        savedEx.push(e);
    }
    
    protected boolean tryWith(AuthMethod meth) throws UserAuthException, TransportException
    {
        meth.init(this);
        result.clear();
        meth.request();
        return result.get(timeout);
    }
    
}
