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
package org.apache.commons.net.ssh.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.net.ssh.ErrorNotifiable;
import org.apache.commons.net.ssh.SSHException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents future data of the parameterzed type {@code V} and allows waiting on it. An exception
 * may also be delivered to a waiter, and will be of the parameterized type {@code T}.
 * <p>
 * For atomic operations on a future - e.g. checking checking if a value is set and if it is not
 * then setting it, i.e. Compare-And-Set type operations - the associated lock for the future should
 * be acquired while doing so.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class Future<V, T extends Throwable> implements ErrorNotifiable
{
    
    public static class FutureException extends Exception
    {
        public FutureException(String message)
        {
            super(message);
        }
    }
    
    protected final Logger log;
    
    protected final FriendlyChainer<T> chainer;
    protected final ReentrantLock lock;
    protected final Condition cond;
    
    protected V val;
    protected T pendingEx;
    
    public Future(String name, FriendlyChainer<T> chainer)
    {
        this(name, chainer, null);
    }
    
    public Future(String name, FriendlyChainer<T> chainer, ReentrantLock lock)
    {
        this.log = LoggerFactory.getLogger("<< " + name + " >>");
        this.chainer = chainer;
        this.lock = lock == null ? new ReentrantLock() : lock;
        this.cond = this.lock.newCondition();
    }
    
    public void clear()
    {
        set(null);
    }
    
    public void error(String message)
    {
        error(new FutureException(message));
    }
    
    public void error(Throwable t)
    {
        lock();
        try {
            pendingEx = chainer.chain(t);
            cond.signalAll();
        } finally {
            unlock();
        }
    }
    
    public V get() throws T
    {
        return get(0);
    }
    
    public V get(int timeout) throws T
    {
        lock();
        try {
            if (val != null)
                return val;
            if (pendingEx != null)
                throw pendingEx;
            log.debug("Awaiting");
            while (val == null && pendingEx == null)
                if (timeout == 0)
                    cond.await();
                else if (!cond.await(timeout, TimeUnit.SECONDS))
                    chainer.chain(new FutureException("Timeout expired"));
            if (pendingEx != null) {
                log.error("Woke to: {}", pendingEx.toString());
                throw pendingEx;
            }
        } catch (InterruptedException ie) {
            throw chainer.chain(ie);
        } finally {
            unlock();
        }
        return val;
    }
    
    public Lock getLock()
    {
        return lock;
    }
    
    public boolean hasError()
    {
        lock();
        try {
            return pendingEx != null;
        } finally {
            unlock();
        }
    }
    
    public boolean hasWaiters()
    {
        lock();
        try {
            return lock.hasWaiters(cond);
        } finally {
            unlock();
        }
    }
    
    public boolean isSet()
    {
        lock();
        try {
            return val != null;
        } finally {
            unlock();
        }
    }
    
    public void lock()
    {
        lock.lock();
    }
    
    public void notifyError(SSHException error)
    {
        error(error);
    }
    
    public void set(V val)
    {
        lock();
        try {
            log.debug("Setting to `{}`", val);
            this.val = val;
            cond.signalAll();
        } finally {
            unlock();
        }
    }
    
    public void unlock()
    {
        lock.unlock();
    }
    
}
