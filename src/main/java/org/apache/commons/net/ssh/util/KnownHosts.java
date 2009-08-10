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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.security.PublicKey;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.net.ssh.HostKeyVerifier;
import org.apache.commons.net.ssh.SSHException;
import org.apache.commons.net.ssh.SSHRuntimeException;
import org.apache.commons.net.ssh.mac.HMACSHA1;
import org.apache.commons.net.ssh.mac.MAC;
import org.apache.commons.net.ssh.util.Constants.KeyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link HostKeyVerifier} implementation for a {@code known_hosts} file i.e. in the format used
 * by OpenSSH.
 * <p>
 * Hashed hostnames are correctly handled.
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 * @see <a href="http://nms.lcs.mit.edu/projects/ssh/README.hashed-hosts">Hashed hostnames spec</a>
 */
public class KnownHosts implements HostKeyVerifier
{
    
    /**
     * Represents a single line
     * 
     * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
     */
    protected static class Entry
    {
        
        private final String[] hosts;
        private final KeyType type;
        private final String sKey;
        private PublicKey key;
        
        /**
         * Construct an entry from a string containing the line
         * 
         * @param line
         *            the line from a known_hosts file
         * @throws SSHException
         *             if it could not be parsed for any reason
         */
        Entry(String line) throws SSHException
        {
            String[] parts = line.split(" ");
            if (parts.length != 3)
                throw new SSHException("Line parts not 3");
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
         * @return the possibility which was successfuly matched, or {@code null} if there was no
         *         match
         */
        public String appliesTo(Set<String> possibilities)
        {
            if (hosts[0].startsWith("|1|")) { // hashed hostname
                String[] splitted = hosts[0].split("\\|");
                if (splitted.length != 4)
                    return null;
                byte[] salt, host;
                try {
                    salt = Base64.decode(splitted[2]);
                    host = Base64.decode(splitted[3]);
                } catch (IOException e) {
                    throw new SSHRuntimeException(e);
                }
                if (salt.length != 20)
                    return null;
                MAC sha1 = new HMACSHA1();
                sha1.init(salt);
                for (String possi : possibilities)
                    if (BufferUtils.equals(host, sha1.doFinal(possi.getBytes())))
                        return possi;
            } else
                // unhashed; possibly comma-delim'ed
                for (String host : hosts)
                    if (possibilities.contains(host))
                        return host;
            return null;
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
            if (key == null) {
                byte[] decoded;
                try {
                    decoded = Base64.decode(sKey);
                } catch (IOException e) {
                    return null;
                }
                key = new Buffer(decoded).getPublicKey();
            }
            return key;
        }
        
        public KeyType getType()
        {
            return type;
        }
        
        @Override
        public String toString()
        {
            String s = hosts[0];
            for (int i = 1; i < hosts.length; i++)
                s += "," + hosts[i];
            s += " " + type.toString();
            s += " " + sKey;
            return s;
        }
    }
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final List<Entry> entries = new LinkedList<Entry>();
    
    public KnownHosts(InputStream stream) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        String line;
        try {
            // read in the file, storing each line as an entry
            while ((line = br.readLine()) != null)
                try {
                    entries.add(new Entry(line));
                } catch (SSHException ignore) {
                    log.debug("Bad line ({}): {} ", ignore.toString(), line);
                    continue;
                }
        } finally {
            IOUtils.closeQuietly(br);
        }
    }
    
    /**
     * Constructs a {@code KnownHosts} object from a file location
     * 
     * @param loc
     *            the file location
     * @throws IOException
     *             if there is an error reading the file
     */
    public KnownHosts(String loc) throws IOException
    {
        this(new FileInputStream(loc));
    }
    
    public List<Entry> getEntries()
    {
        return Collections.unmodifiableList(entries);
    }
    
    /**
     * Checks whether the specified host is known per the contents of the {@code known_hosts} file.
     * 
     * @return {@code true} on successful verfication or {@code false} on failure
     */
    public boolean verify(InetAddress host, PublicKey key)
    {
        KeyType type = KeyType.fromKey(key);
        if (type == KeyType.UNKNOWN)
            return false;
        
        Set<String> possibilities = new HashSet<String>();
        possibilities.add(host.getHostName());
        
        /*
         * Commented out because not sure of security implications
         */
        //possibilities.add(host.getCanonicalHostName());
        //possibilities.add(host.getHostAddress());
        
        log.debug("Checking for any of {}", possibilities);
        
        for (Entry e : entries) {
            String match;
            if (e.getType() == type && (match = e.appliesTo(possibilities)) != null)
                if (key.equals(e.getKey())) {
                    log.info("Matched against `{}`", match);
                    return true;
                } else {
                    log.warn("Host key for `{}` has changed!", match);
                    return false;
                }
        }
        return false;
    }
    
}
