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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.net.ssh.cipher.Cipher;
import org.apache.commons.net.ssh.compression.Compression;
import org.apache.commons.net.ssh.kex.KeyExchange;
import org.apache.commons.net.ssh.keyprovider.FileKeyProvider;
import org.apache.commons.net.ssh.mac.MAC;
import org.apache.commons.net.ssh.random.Random;
import org.apache.commons.net.ssh.signature.Signature;

/**
 * Holds configuration information and factories. Acts a container for factories of
 * {@link KeyExchange}, {@link Cipher}, {@link Compression}, {@link MAC}, {@link Signature},
 * {@link Random}, and {@link FileKeyProvider}.
 */
public class Config
{
    
    private String version;
    
    private Factory<Random> randomFactory;
    
    private List<Factory.Named<KeyExchange>> kexFactories;
    private List<Factory.Named<Cipher>> cipherFactories;
    private List<Factory.Named<Compression>> compressionFactories;
    private List<Factory.Named<MAC>> macFactories;
    private List<Factory.Named<Signature>> signatureFactories;
    private List<Factory.Named<FileKeyProvider>> fileKeyProviderFactories;
    
    /**
     * Retrieve the list of named factories for {@code Cipher}.
     * 
     * @return a list of named {@code Cipher} factories
     */
    public List<Factory.Named<Cipher>> getCipherFactories()
    {
        return cipherFactories;
    }
    
    /**
     * Retrieve the list of named factories for {@code Compression}.
     * 
     * @return a list of named {@code Compression} factories
     */
    public List<Factory.Named<Compression>> getCompressionFactories()
    {
        return compressionFactories;
    }
    
    /**
     * Retrieve the list of named factories for {@code FileKeyProvider}.
     * 
     * @return a list of named {@code FileKeyProvider} factories
     */
    public List<Factory.Named<FileKeyProvider>> getFileKeyProviderFactories()
    {
        return fileKeyProviderFactories;
    }
    
    /**
     * Retrieve the list of named factories for <code>KeyExchange</code>.
     * 
     * @return a list of named <code>KeyExchange</code> factories
     */
    public List<Factory.Named<KeyExchange>> getKeyExchangeFactories()
    {
        return kexFactories;
    }
    
    /**
     * Retrieve the list of named factories for <code>MAC</code>.
     * 
     * @return a list of named <code>MAC</code> factories
     */
    public List<Factory.Named<MAC>> getMACFactories()
    {
        return macFactories;
    }
    
    /**
     * Retrieve the {@link Random} factory.
     * 
     * @return the {@link Random} factory
     */
    public Factory<Random> getRandomFactory()
    {
        return randomFactory;
    }
    
    /**
     * Retrieve the list of named factories for {@link Signature}
     * 
     * @return a list of named {@link Signature} factories
     */
    public List<Factory.Named<Signature>> getSignatureFactories()
    {
        return signatureFactories;
    }
    
    /**
     * Returns the software version information for identification during SSH connection
     * initialization. For example, {@code "NET_3_0"}.
     */
    public String getVersion()
    {
        return version;
    }
    
    /**
     * Set the named factories for {@link Cipher}.
     * 
     * @param cipherFactories
     *            any number of named factories
     */
    public void setCipherFactories(Factory.Named<Cipher>... cipherFactories)
    {
        setCipherFactories(Arrays.<Factory.Named<Cipher>> asList(cipherFactories));
    }
    
    /**
     * Set the named factories for {@link Cipher}.
     * 
     * @param cipherFactories
     *            a list of named factories
     */
    public void setCipherFactories(List<Factory.Named<Cipher>> cipherFactories)
    {
        this.cipherFactories = cipherFactories;
    }
    
    /**
     * Set the named factories for {@link Compression}.
     * 
     * @param compressionFactories
     *            any number of named factories
     */
    public void setCompressionFactories(Factory.Named<Compression>... compressionFactories)
    {
        setCompressionFactories(Arrays.<Factory.Named<Compression>> asList(compressionFactories));
    }
    
    /**
     * Set the named factories for {@link Compression}.
     * 
     * @param compressionFactories
     *            a list of named factories
     */
    public void setCompressionFactories(List<Factory.Named<Compression>> compressionFactories)
    {
        this.compressionFactories = compressionFactories;
    }
    
    /**
     * Set the named factories for {@link FileKeyProvider}.
     * 
     * @param fileKeyProviderFactories
     *            any number of named factories
     */
    public void setFileKeyProviderFactories(Factory.Named<FileKeyProvider>... fileKeyProviderFactories)
    {
        setFileKeyProviderFactories(Arrays.<Factory.Named<FileKeyProvider>> asList(fileKeyProviderFactories));
    }
    
    /**
     * Set the named factories for {@link FileKeyProvider}.
     * 
     * @param fileKeyProviderFactories
     *            a list of named factories
     */
    public void setFileKeyProviderFactories(List<Factory.Named<FileKeyProvider>> fileKeyProviderFactories)
    {
        this.fileKeyProviderFactories = fileKeyProviderFactories;
    }
    
    /**
     * Set the named factories for {@link KeyExchange}.
     * 
     * @param kexFactories
     *            any number of named factories
     */
    public void setKeyExchangeFactories(Factory.Named<KeyExchange>... kexFactories)
    {
        setKeyExchangeFactories(Arrays.<Factory.Named<KeyExchange>> asList(kexFactories));
    }
    
    /**
     * Set the named factories for {@link KeyExchange}.
     * 
     * @param kexFactories
     *            a list of named factories
     */
    public void setKeyExchangeFactories(List<Factory.Named<KeyExchange>> kexFactories)
    {
        this.kexFactories = kexFactories;
    }
    
    /**
     * Set the named factories for {@link MAC}.
     * 
     * @param macFactories
     *            any number of named factories
     */
    public void setMACFactories(Factory.Named<MAC>... macFactories)
    {
        setMACFactories(Arrays.<Factory.Named<MAC>> asList(macFactories));
    }
    
    /**
     * Set the named factories for {@link MAC}.
     * 
     * @param macFactories
     *            a list of named factories
     */
    public void setMACFactories(List<Factory.Named<MAC>> macFactories)
    {
        this.macFactories = macFactories;
    }
    
    /**
     * Set the factory for {@link Random}.
     * 
     * @param randomFactory
     *            the factory
     */
    public void setRandomFactory(Factory<Random> randomFactory)
    {
        this.randomFactory = randomFactory;
    }
    
    /**
     * Set the named factories for {@link Signature}.
     * 
     * @param signatureFactories
     *            any number of named factories
     */
    public void setSignatureFactories(Factory.Named<Signature>... signatureFactories)
    {
        setSignatureFactories(Arrays.<Factory.Named<Signature>> asList(signatureFactories));
    }
    
    /**
     * Set the named factories for {@link Signature}.
     * 
     * @param signatureFactories
     *            a list of named factories
     */
    public void setSignatureFactories(List<Factory.Named<Signature>> signatureFactories)
    {
        this.signatureFactories = signatureFactories;
    }
    
    /**
     * Set the software version information for identification during SSH connection initialization.
     * For example, {@code "NET_3_0"}.
     * 
     * @param version
     *            software version info
     */
    public void setVersion(String version)
    {
        this.version = version;
    }
    
}