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
public class Event<Ex extends Throwable> extends Future<Boolean, Ex>
{
    
    public static class EventException extends Exception
    {
        public EventException(String message)
        {
            super(message);
        }
    }
    
    public Event(String name, FriendlyChainer<Ex> chainer)
    {
        super(name, chainer, null);
    }
    
    public Event(String name, FriendlyChainer<Ex> chainer, ReentrantLock lock)
    {
        super(name, chainer, lock);
    }
    
    public void await() throws Ex
    {
        super.get();
    }
    
    public void await(int timeout) throws Ex
    {
        super.get(timeout);
    }
    
    public void set()
    {
        super.set(true);
    }
    
}
