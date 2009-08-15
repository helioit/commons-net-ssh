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

import java.util.concurrent.locks.ReentrantLock;

/*
 * Syntactic sugar around Future
 */

/**
 * A type of {@link Future} that caters to boolean values. Similar to Python's {@code
 * threading.event}, with the key difference that a waiter may be delivered an exception of type
 * {@code T}.
 * 
 * @see Future
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class Event<T extends Throwable> extends Future<Boolean, T>
{
    
    public static class EventException extends Exception
    {
        public EventException(String message)
        {
            super(message);
        }
    }
    
    /**
     * Creates this event with given {@code name} and exception {@code chainer}. Allocates a new
     * {@link java.util.concurrent.locks.Lock lock} object for this event.
     * 
     * @param name
     *            name of this event
     * @param chainer
     *            {@link FriendlyChainer} that will be used for chaining exceptions
     */
    public Event(String name, FriendlyChainer<T> chainer)
    {
        super(name, chainer);
    }
    
    /**
     * 
     * @param name
     * @param chainer
     * @param lock
     */
    public Event(String name, FriendlyChainer<T> chainer, ReentrantLock lock)
    {
        super(name, chainer, lock);
    }
    
    public void await() throws T
    {
        super.get();
    }
    
    public void await(int timeout) throws T
    {
        super.get(timeout);
    }
    
    public void set()
    {
        super.set(true);
    }
    
}
