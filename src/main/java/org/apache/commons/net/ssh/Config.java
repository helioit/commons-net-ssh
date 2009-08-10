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

import org.apache.commons.net.ssh.Factory.Named;
import org.apache.commons.net.ssh.cipher.Cipher;
import org.apache.commons.net.ssh.compression.Compression;
import org.apache.commons.net.ssh.kex.KeyExchange;
import org.apache.commons.net.ssh.keyprovider.FileKeyProvider;
import org.apache.commons.net.ssh.mac.MAC;
import org.apache.commons.net.ssh.random.Random;
import org.apache.commons.net.ssh.signature.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds configuration information, implementations of core classes, and factories.
 * <p>
 * This is a container for {@link Named} implementations of {@link KeyExchange}, {@link Cipher},
 * {@link Compression}, {@link MAC}, {@link Signature}, {@link Random}, and {@link FileKeyProvider}.
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class Config
{
    
    protected static final Logger log = LoggerFactory.getLogger(Config.class);
    
    protected String version;
    
    protected Factory<Random> randomFactory;
    
    protected List<Factory.Named<KeyExchange>> kexFactories;
    protected List<Factory.Named<Cipher>> cipherFactories;
    protected List<Factory.Named<Compression>> compressionFactories;
    protected List<Factory.Named<MAC>> macFactories;
    protected List<Factory.Named<Signature>> signatureFactories;
    protected List<Factory.Named<FileKeyProvider>> fileKeyProviderFactories;
    
    /**
     * Retrieve the list of named factories for <code>Cipher</code>.
     * 
     * @return a list of named <code>Cipher</code> factories, never {@code null}
     */
    public List<Factory.Named<Cipher>> getCipherFactories()
    {
        return cipherFactories;
    }
    
    /**
     * Retrieve the list of named factories for <code>Compression</code>.
     * 
     * @return a list of named {@code Compression} factories, never {@code null}
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
     * @return a list of named <code>KeyExchange</code> factories, never {@code null}
     */
    public List<Factory.Named<KeyExchange>> getKeyExchangeFactories()
    {
        return kexFactories;
    }
    
    /**
     * Retrieve the list of named factories for <code>MAC</code>.
     * 
     * @return a list of named <code>Mac</code> factories, never {@code null}
     */
    public List<Factory.Named<MAC>> getMACFactories()
    {
        return macFactories;
    }
    
    /**
     * Retrieve the {@link Random} factory to be used.
     * 
     * @return the {@link Random} factory, never {@code null}
     */
    public Factory<Random> getRandomFactory()
    {
        return randomFactory;
    }
    
    /**
     * Retrieve the list of named factories for {@link Signature}
     * 
     * @return a list of named {@link Signature} factories, never {@code null}
     */
    public List<Factory.Named<Signature>> getSignatureFactories()
    {
        return signatureFactories;
    }
    
    public String getVersion()
    {
        return version;
    }
    
    public void setCipherFactories(Factory.Named<Cipher>... cipherFactories)
    {
        setCipherFactories(Arrays.<Factory.Named<Cipher>> asList(cipherFactories));
    }
    
    public void setCipherFactories(List<Factory.Named<Cipher>> cipherFactories)
    {
        this.cipherFactories = cipherFactories;
    }
    
    public void setCompressionFactories(Factory.Named<Compression>... compressionFactories)
    {
        setCompressionFactories(Arrays.<Factory.Named<Compression>> asList(compressionFactories));
    }
    
    public void setCompressionFactories(List<Factory.Named<Compression>> compressionFactories)
    {
        this.compressionFactories = compressionFactories;
    }
    
    public void setFileKeyProviderFactories(Factory.Named<FileKeyProvider>... fileKeyProviderFactories)
    {
        setFileKeyProviderFactories(Arrays.<Factory.Named<FileKeyProvider>> asList(fileKeyProviderFactories));
    }
    
    public void setFileKeyProviderFactories(List<Factory.Named<FileKeyProvider>> fileKeyProviderFactories)
    {
        this.fileKeyProviderFactories = fileKeyProviderFactories;
    }
    
    public void setKeyExchangeFactories(Factory.Named<KeyExchange>... kexFactories)
    {
        setKeyExchangeFactories(Arrays.<Factory.Named<KeyExchange>> asList(kexFactories));
    }
    
    public void setKeyExchangeFactories(List<Factory.Named<KeyExchange>> kexFactories)
    {
        this.kexFactories = kexFactories;
    }
    
    public void setMACFactories(Factory.Named<MAC>... macFactories)
    {
        setMACFactories(Arrays.<Factory.Named<MAC>> asList(macFactories));
    }
    
    public void setMACFactories(List<Factory.Named<MAC>> macFactories)
    {
        this.macFactories = macFactories;
    }
    
    public void setRandomFactory(Factory<Random> prngFactory)
    {
        this.randomFactory = prngFactory;
    }
    
    public void setSignatureFactories(Factory.Named<Signature>... signatureFactories)
    {
        setSignatureFactories(Arrays.<Factory.Named<Signature>> asList(signatureFactories));
    }
    
    public void setSignatureFactories(List<Factory.Named<Signature>> signatureFactories)
    {
        this.signatureFactories = signatureFactories;
    }
    
    public void setVersion(String version)
    {
        this.version = version;
    }
    
}
