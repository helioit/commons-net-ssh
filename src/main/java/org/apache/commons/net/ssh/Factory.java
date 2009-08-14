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

import java.util.List;

/**
 * A basic factory interface.
 * 
 * @param <T>
 *            the type of instances created by this factory
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public interface Factory<T>
{
    
    /**
     * A named factory is a factory identified by a name. Such names are used mainly in the
     * algorithm negotiation at the beginning of the SSH connection.
     * 
     * @param <T>
     *            type of object created by this factory
     */
    interface Named<T> extends Factory<T>
    {
        
        /**
         * Utility functions for ease-of-dealing with named factories..
         */
        public static class Util
        {
            
            /**
             * Create an instance of the specified name by looking up the needed factory in the
             * list.
             * 
             * @param factories
             *            list of available factories
             * @param name
             *            the factory name to use
             * @param <T>
             *            type of object to create
             * @return a newly created object or <code>null</code> if the factory is not in the list
             */
            public static <T> T create(List<Named<T>> factories, String name)
            {
                if (factories != null)
                    for (Named<T> f : factories)
                        if (f.getName().equals(name))
                            return f.create();
                return null;
            }
            
            /**
             * Retrieve the factory identified by its name from the list.
             * 
             * @param factories
             *            list of available factories
             * @param name
             *            the name of the factory to retrieve
             * @param <T>
             *            type of object created by the factories
             * @return a factory or <code>null</code> if not found in the list
             */
            public static <T> Named<T> get(List<Named<T>> factories, String name)
            {
                for (Named<T> f : factories)
                    if (f.getName().equals(name))
                        return f;
                return null;
            }
            
            /**
             * Get a comma separated list of the factory names from the given list.
             * 
             * @param factories
             *            list of available factories
             * @param <T>
             *            type of object to create
             * @return a comma separated list of factory names
             */
            public static <T> String getNames(List<Named<T>> factories)
            {
                StringBuffer sb = new StringBuffer();
                for (Named<T> f : factories) {
                    if (sb.length() > 0)
                        sb.append(",");
                    sb.append(f.getName());
                }
                return sb.toString();
            }
            
            /**
             * Remove the factory identified by the name from the list.
             * 
             * @param factories
             *            list of factories
             * @param name
             *            the name of the factory to remove
             * @param <T>
             *            type of object to create
             * @return the factory removed from the list or <code>null</code> if not in the list
             */
            public static <T> Named<T> remove(List<Named<T>> factories, String name)
            {
                for (Named<T> f : factories)
                    if (f.getName().equals(name)) {
                        factories.remove(f);
                        return f;
                    }
                return null;
            }
            
        }
        
        /**
         * Name of this factory
         */
        String getName();
        
    }
    
    /**
     * Create a new instance
     */
    T create();
    
}
