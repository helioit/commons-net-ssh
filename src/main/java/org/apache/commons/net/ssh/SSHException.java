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
package org.apache.commons.net.ssh;

import java.io.IOException;

import org.apache.commons.net.ssh.util.FriendlyChainer;
import org.apache.commons.net.ssh.util.Constants.DisconnectReason;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public class SSHException extends IOException
{
    
    public static final FriendlyChainer<SSHException> chainer = new FriendlyChainer<SSHException>()
        {
            
            public SSHException chain(Throwable t)
            {
                if (t instanceof SSHException)
                    return (SSHException) t;
                else
                    return new SSHException(t);
            }
            
        };
    
    private final DisconnectReason code;
    
    public SSHException()
    {
        this(DisconnectReason.UNKNOWN, null, null);
    }
    
    public SSHException(DisconnectReason code)
    {
        this(code, null, null);
    }
    
    public SSHException(DisconnectReason code, String message)
    {
        this(code, message, null);
    }
    
    public SSHException(DisconnectReason code, String message, Throwable cause)
    {
        super(message);
        this.code = code;
        if (cause != null)
            initCause(cause);
    }
    
    public SSHException(DisconnectReason code, Throwable cause)
    {
        this(code, null, cause);
    }
    
    public SSHException(String message)
    {
        this(DisconnectReason.UNKNOWN, message, null);
    }
    
    public SSHException(String message, Throwable cause)
    {
        this(DisconnectReason.UNKNOWN, message, cause);
    }
    
    public SSHException(Throwable cause)
    {
        this(DisconnectReason.UNKNOWN, null, cause);
    }
    
    public int getDisconnectCode()
    {
        return code.toInt();
    }
    
    public DisconnectReason getDisconnectReason()
    {
        return code;
    }
    
    @Override
    public String getMessage()
    {
        if (super.getMessage() != null)
            return super.getMessage();
        else if (getCause() != null && getCause().getMessage() != null)
            return getCause().getMessage();
        else
            return super.toString();
    }
    
    @Override
    public String toString()
    {
        return (code != DisconnectReason.UNKNOWN ? "[" + code + "] " : "") + getMessage();
    }
    
}
