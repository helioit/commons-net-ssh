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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.net.ssh.HostKeyVerifier;
import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.SSHRuntimeException;
import org.apache.commons.net.ssh.mac.HMACSHA1;
import org.apache.commons.net.ssh.mac.MAC;
import org.apache.commons.net.ssh.util.Buffer.PlainBuffer;
import org.apache.commons.net.ssh.util.Constants.KeyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: allow modifications to known_hosts e.g. adding entries

/**
 * A {@link HostKeyVerifier} implementation for a {@code known_hosts} file i.e. in the format used by OpenSSH.
 * <p>
 * Hashed hostnames are correctly handled.
 * 
 * @see <a href="http://nms.lcs.mit.edu/projects/ssh/README.hashed-hosts">Hashed hostnames spec</a>
 */
public class KnownHosts implements HostKeyVerifier
{
    
    private static final String LS = System.getProperty("line.separator");
    
    /**
     * Represents a single line
     */
    public static class Entry
    {
        private final String[] hosts;
        private final KeyType type;
        private String sKey;
        private PublicKey key;
        
        private final MAC sha1 = new HMACSHA1();
        
        /**
         * Construct an entry from the hostname and public key
         */
        public Entry(String hostname, PublicKey key)
        {
            this.key = key;
            hosts = new String[] { hostname };
            type = KeyType.fromKey(key);
        }
        
        /**
         * Construct an entry from a string containing the line
         * 
         * @param line
         *            the line from a known_hosts file
         * @throws SSHException
         *             if it could not be parsed for any reason
         */
        public Entry(String line) throws SSHException
        {
            String[] parts = line.split(" ");
            if (parts.length != 3)
                throw new SSHException("Line parts not 3: " + line);
            hosts = parts[0].split(",");
            type = KeyType.fromString(parts[1]);
            if (type == KeyType.UNKNOWN)
                throw new SSHException("Unknown key type: " + parts[1]);
            sKey = parts[2];
        }
        
        /**
         * Checks whether this entry is applicable to any of {@code possibilities}
         * 
         * @param possibilities
         *            a set of possibilities to check against
         * @return the possibility which was successfully matched, or {@code null} if there was no match
         */
        public boolean appliesTo(String hostname)
        {
            if (hosts[0].startsWith("|1|"))
            { // hashed hostname
                String[] splitted = hosts[0].split("\\|");
                if (splitted.length != 4)
                    return false;
                byte[] salt, host;
                try
                {
                    salt = Base64.decode(splitted[2]);
                    host = Base64.decode(splitted[3]);
                } catch (IOException e)
                {
                    throw new SSHRuntimeException(e);
                }
                if (salt.length != 20)
                    return false;
                sha1.init(salt);
                if (BufferUtils.equals(host, sha1.doFinal(hostname.getBytes())))
                    return true;
            } else
                // unhashed; possibly comma-delim'ed
                for (String host : hosts)
                    if (host.equals(hostname))
                        return true;
            return false;
        }
        
        /**
         * Returns the public host key represented in this entry.
         * <p>
         * The key is cached so repeated calls to this method may be made without concern.
         * 
         * @return the host key
         */
        public PublicKey getKey()
        {
            if (key == null)
            {
                byte[] decoded;
                try
                {
                    decoded = Base64.decode(sKey);
                } catch (IOException e)
                {
                    return null;
                }
                key = new PlainBuffer(decoded).readPublicKey();
            }
            return key;
        }
        
        public KeyType getType()
        {
            return type;
        }
        
        public String getLine()
        {
            String s = hosts[0];
            for (int i = 1; i < hosts.length; i++)
                s += "," + hosts[i];
            s += " " + type.toString();
            if (sKey == null)
            {
                PlainBuffer buf = new PlainBuffer().putPublicKey(key);
                sKey = Base64.encodeBytes(buf.array(), buf.rpos(), buf.available());
            }
            s += " " + sKey;
            return s;
        }
        
        @Override
        public String toString()
        {
            return "Entry{hostnames=" + Arrays.toString(hosts) + "; type=" + type + "; key=" + getKey() + "}";
        }
        
        // IMPLEMENT HashCode & equals()
        
    }
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final File khFile;
    private final Set<Entry> entries = new LinkedHashSet<Entry>();
    
    /**
     * Constructs a {@code KnownHosts} object from a file location
     * 
     * @param khFile
     *            the file location
     * @throws IOException
     *             if there is an error reading the file
     */
    public KnownHosts(File khFile) throws IOException
    {
        this.khFile = khFile;
        if (khFile.exists())
        {
            BufferedReader br = new BufferedReader(new FileReader(khFile));
            String line;
            try
            {
                // Read in the file, storing each line as an entry
                while ((line = br.readLine()) != null)
                    try
                    {
                        entries.add(new Entry(line));
                    } catch (SSHException ignore)
                    {
                        log.debug("Bad line ({}): {} ", ignore.toString(), line);
                        continue;
                    }
            } finally
            {
                IOUtils.closeQuietly(br);
            }
        }
    }
    
    /**
     * Checks whether the specified hostname is known per the contents of the {@code known_hosts} file.
     * 
     * @return {@code true} on successful verification or {@code false} on failure
     */
    public boolean verify(String hostname, PublicKey key)
    {
        KeyType type = KeyType.fromKey(key);
        if (type == KeyType.UNKNOWN)
            return false;
        
        for (Entry e : entries)
            if (e.getType() == type && e.appliesTo(hostname))
                if (key.equals(e.getKey()))
                    return true;
                else
                {
                    log.warn("Host key for `{}` has changed!", hostname);
                    return false;
                }
        return false;
    }
    
    public Set<Entry> entries()
    {
        return Collections.unmodifiableSet(entries);
    }
    
    public void write() throws IOException
    {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(khFile));
        for (Entry entry : entries)
            bos.write((entry.getLine() + LS).getBytes());
        bos.close();
    }
}
