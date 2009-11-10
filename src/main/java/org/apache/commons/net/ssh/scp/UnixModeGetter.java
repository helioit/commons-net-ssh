/*
 * Licensed to the Apache Software Founimport java.io.File;
import java.io.IOException;
ense agreements.  See the NOTICE file
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
 * Use Runtime.exec() to run unix commands for getting these values 
 */

/**
 * STUB
 */
public class UnixModeGetter implements ModeGetter
{
    
    public long getLastAccessTime(File f) throws IOException
    {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public long getLastModifiedTime(File f) throws IOException
    {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public String getPermissions(File f) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public boolean shouldPreserveTimes()
    {
        return true;
    }
    
}
