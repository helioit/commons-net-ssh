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

import java.util.Arrays;
import java.util.Map;

/**
 * An interface for servicing requests for plaintext passwords.
 */
public interface PasswordFinder
{
    
    /**
     * A password-protected resource
     */
    class Resource
    {
        
        public enum Type
        {
            /**
             * The password-protected resource is an account
             * 
             * Corresponding detail: user@hostname
             */
            ACCOUNT,

            /**
             * The password-protected resource is a private key file
             * 
             * Corresponding detail: /file/path
             */
            KEYFILE,
        }
        
        private final Type type;
        private final String detail;
        
        public Resource(Type type, String detail)
        {
            this.type = type;
            this.detail = detail;
        }
        
        @Override
        public boolean equals(Object r2)
        {
            if (r2 instanceof Resource)
                return ((Resource) r2).type == type && ((Resource) r2).detail.equals(detail);
            else
                return false;
        }
        
        public String getDetail()
        {
            return detail;
        }
        
        public Type getType()
        {
            return type;
        }
        
        @Override
        public int hashCode()
        {
            return (type + detail).hashCode();
        }
        
        @Override
        public String toString()
        {
            return "[" + type + "] " + detail;
        }
        
    }
    
    /**
     * Static utility methods and factories
     */
    class Util
    {
        
        /**
         * Blank out a character array
         * 
         * @param pwd
         *            the character array
         */
        public static void blankOut(char[] pwd)
        {
            if (pwd != null)
                Arrays.fill(pwd, ' ');
        }
        
        /**
         * @param password
         *            the password as a char[]
         * @return the constructed {@link PasswordFinder}
         */
        public static PasswordFinder createOneOff(final char[] password)
        {
            if (password == null)
                return null;
            else
                return new PasswordFinder()
                {
                    public char[] reqPassword(Resource resource)
                    {
                        char[] cloned = password.clone();
                        blankOut(password);
                        return cloned;
                    }
                    
                    public boolean shouldRetry(Resource resource)
                    {
                        return false;
                    }
                };
        }
        
        public static PasswordFinder createResourceBased(final Map<Resource, String> passwordMap)
        {
            return new PasswordFinder()
            {
                public char[] reqPassword(Resource resource)
                {
                    return passwordMap.get(resource).toCharArray();
                }
                
                public boolean shouldRetry(Resource resource)
                {
                    return false;
                }
                
            };
        }
    }
    
    /**
     * Request password for specified resource.
     * <p>
     * This method may return {@code null} when the request cannot be serviced, e.g. when the user
     * cancels a password prompt. The consequences of returning {@code null} are specific to the
     * requestor.
     * 
     * @param resource
     *            the resource for which password is being requested
     * @return the password or {@code null}
     */
    char[] reqPassword(Resource resource);
    
    /**
     * If password turns out to be incorrect, indicates whether another call to
     * {@link #reqPassword(Resource)} should be made.
     * <p>
     * This method is geared at interactive implementations, and stub implementations may simply
     * return {@code false}.
     * 
     * @return whether to retry requesting password for a particular resource
     */
    boolean shouldRetry(Resource resource);
    
}
