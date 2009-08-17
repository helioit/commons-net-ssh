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

/**
 * API for classes that are capable of being notified on an error so they can cleanup.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public interface ErrorNotifiable
{
    
    /**
     * Utility functions.
     */
    class Util
    {
        /**
         * Notify all {@code notifiables} of given {@code error}.
         * <p>
         * The signature states {@code Object...} for convenience but all {@code notifiables} must
         * be instances of {@link ErrorNotifiable}.
         */
        public static void alertAll(SSHException error, Object... notifiables)
        {
            for (Object notifiable : notifiables)
                if (notifiable instanceof ErrorNotifiable)
                    ((ErrorNotifiable) notifiable).notifyError(error);
        }
    }
    
    /**
     * Notifies this object of an {@code error}.
     */
    void notifyError(SSHException error);
    
}
