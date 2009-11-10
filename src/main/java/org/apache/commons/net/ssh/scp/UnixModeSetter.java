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
package org.apache.commons.net.ssh.scp;

import java.io.File;
import java.io.IOException;

/*
 * TODO
 * Use Runtime.exec() to run unix commands for setting these values 
 */

/**
 * STUB
 */
public class UnixModeSetter implements ModeSetter
{
    
    public void setLastAccessedTime(File f, long t) throws IOException
    {
        // TODO Auto-generated method stub
        
    }
    
    public void setLastModifiedTime(File f, long t) throws IOException
    {
        // TODO Auto-generated method stub
        
    }
    
    public void setPermissions(File f, String perms) throws IOException
    {
        // TODO Auto-generated method stub
        
    }
    
    public boolean shouldPreserveTimes()
    {
        return true;
    }
    
}