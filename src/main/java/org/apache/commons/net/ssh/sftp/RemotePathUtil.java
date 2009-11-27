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
package org.apache.commons.net.ssh.sftp;

import java.io.IOException;

class RemotePathUtil
{
    
    private final SFTP sftp;
    private String dotDir;
    
    public RemotePathUtil(SFTP sftp)
    {
        this.sftp = sftp;
    }
    
    private String canon(String path) throws IOException
    {
        if (path.equals("."))
            return (dotDir != null) ? dotDir : (dotDir = sftp.canonicalize("."));
        else
            return sftp.canonicalize(path);
    }
    
    public PathComponents getComponents(String path) throws IOException
    {
        if (path.isEmpty())
            return getComponents(canon("."));
        
        final int ls = path.lastIndexOf("/");
        
        if (ls == -1)
            if (path.equals(".") || path.equals(".."))
                return getComponents(canon(path));
            else
                return new PathComponents(canon("."), path);
        
        final String name = path.substring(ls + 1);
        
        if (name.equals(".") || name.equals(".."))
            return getComponents(canon(path));
        else
        {
            final String parent = path.substring(0, ls);
            return new PathComponents(parent, name);
        }
    }
    
    public static String adjustForParent(String parent, String path)
    {
        return (path.startsWith("/")) ? path // Absolute path
                : (parent + (parent.endsWith("/") ? "" : "/") + path); // Relative path
    }
    
}