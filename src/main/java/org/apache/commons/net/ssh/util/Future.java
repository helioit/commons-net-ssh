package org.apache.commons.net.ssh.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Future<V, Ex extends Throwable>
{
    
    public static class FutureException extends Exception
    {
        public FutureException(String message)
        {
            super(message);
        }
    }
    
    public static class Util
    {
        public static <V, Ex extends Throwable> void notifyError(Throwable error, Future<V, Ex>... futures)
        {
            for (Future<V, Ex> event : futures)
                event.error(error);
        }
        
        public static <V, Ex extends Throwable> void notifyError(Throwable error,
                Iterable<? extends Future<V, Ex>> futures)
        {
            for (Future<V, Ex> event : futures)
                event.error(error);
        }
    }
    
    protected final Logger log;
    protected final FriendlyChainer<Ex> chainer;
    protected final ReentrantLock lock;
    protected final Condition cond;
    
    protected V val;
    protected Ex pendingEx;
    
    public Future(String name, FriendlyChainer<Ex> chainer, ReentrantLock lock)
    {
        this.log = LoggerFactory.getLogger("<< " + name + " >>");
        this.chainer = chainer;
        this.lock = lock == null ? new ReentrantLock() : lock;
        this.cond = this.lock.newCondition();
    }
    
    public void clear()
    {
        lock.lock();
        try {
            this.val = null;
            cond.signalAll();
        } finally {
            lock.unlock();
        }
    }
    
    public void error(String message)
    {
        error(new FutureException(message));
    }
    
    public void error(Throwable t)
    {
        lock.lock();
        try {
            pendingEx = chainer.chain(t);
            cond.signalAll();
        } finally {
            lock.unlock();
        }
    }
    
    public V get() throws Ex
    {
        return get(0);
    }
    
    public V get(int timeout) throws Ex
    {
        lock.lock();
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
            lock.unlock();
        }
        return val;
    }
    
    public boolean hasError()
    {
        lock.lock();
        try {
            return pendingEx != null;
        } finally {
            lock.unlock();
        }
    }
    
    public boolean hasWaiters()
    {
        lock.lock();
        try {
            return lock.hasWaiters(cond);
        } finally {
            lock.unlock();
        }
    }
    
    public boolean isSet()
    {
        lock.lock();
        try {
            return val != null;
        } finally {
            lock.unlock();
        }
    }
    
    public void set(V val)
    {
        lock.lock();
        try {
            log.debug("Setting");
            this.val = val;
            cond.signalAll();
        } finally {
            lock.unlock();
        }
    }
    
}
