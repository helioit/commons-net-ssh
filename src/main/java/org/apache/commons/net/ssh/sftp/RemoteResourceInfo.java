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

public class RemoteResourceInfo
{
    
    private final String name;
    private final String longName;
    private final FileAttributes attrs;
    
    public RemoteResourceInfo(String name, String longName, FileAttributes attrs)
    {
        this.name = name;
        this.longName = longName;
        this.attrs = attrs;
    }
    
    public String getName()
    {
        return name;
    }
    
    public String getLongName()
    {
        return longName;
    }
    
    public FileAttributes getAttributes()
    {
        return attrs;
    }
    
    public boolean isType(FileMode.Type type)
    {
        return attrs.getType() == type;
    }
    
    public boolean isRegularFile()
    {
        return isType(FileMode.Type.REGULAR);
    }
    
    public boolean isDirectory()
    {
        return isType(FileMode.Type.DIRECTORY);
    }
    
    public boolean isSymlink()
    {
        return isType(FileMode.Type.SYMKLINK);
    }
    
}
