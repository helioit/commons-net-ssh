package org.apache.commons.net.ssh;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.net.ssh.cipher.Cipher;
import org.apache.commons.net.ssh.compression.Compression;
import org.apache.commons.net.ssh.kex.KeyExchange;
import org.apache.commons.net.ssh.keyprovider.FileKeyProvider;
import org.apache.commons.net.ssh.mac.MAC;
import org.apache.commons.net.ssh.prng.PRNG;
import org.apache.commons.net.ssh.signature.Signature;
import org.apache.commons.net.ssh.transport.Decoder;
import org.apache.commons.net.ssh.transport.Encoder;
import org.apache.commons.net.ssh.transport.KeyExchanger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds configuration information, implementations of core classes, and factories.
 * <p>
 * This is a container for {@link NamedFactory} implementations of {@link KeyExchange},
 * {@link Cipher}, {@link Compression}, {@link MAC}, {@link Signature}, {@link PRNG}, and
 * {@link FileKeyProvider}.
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class Config
{
    
    protected static final Logger log = LoggerFactory.getLogger(Config.class);
    
    protected String version;
    protected KeyExchanger kexer;
    protected Encoder encoder;
    protected Decoder decoder;
    
    protected List<NamedFactory<KeyExchange>> keyExchangeFactories;
    protected List<NamedFactory<Cipher>> cipherFactories;
    protected List<NamedFactory<Compression>> compressionFactories;
    protected List<NamedFactory<MAC>> macFactories;
    protected List<NamedFactory<Signature>> signatureFactories;
    protected List<NamedFactory<FileKeyProvider>> fileKeyProviderFactories;
    protected NamedFactory<PRNG> prngFactory;
    
    /**
     * Retrieve the list of named factories for <code>Cipher</code>.
     * 
     * @return a list of named <code>Cipher</code> factories, never {@code null}
     */
    public List<NamedFactory<Cipher>> getCipherFactories()
    {
        return cipherFactories;
    }
    
    /**
     * Retrieve the list of named factories for <code>Compression</code>.
     * 
     * @return a list of named {@code Compression} factories, never {@code null}
     */
    public List<NamedFactory<Compression>> getCompressionFactories()
    {
        return compressionFactories;
    }
    
    public Decoder getDecoder()
    {
        return decoder;
    }
    
    public Encoder getEncoder()
    {
        return encoder;
    }
    
    /**
     * Retrieve the list of named factories for {@code FileKeyProvider}.
     * 
     * @return a list of named {@code FileKeyProvider} factories
     */
    public List<NamedFactory<FileKeyProvider>> getFileKeyProviderFactories()
    {
        return fileKeyProviderFactories;
    }
    
    /**
     * Retrieve the list of named factories for <code>KeyExchange</code>.
     * 
     * @return a list of named <code>KeyExchange</code> factories, never {@code null}
     */
    public List<NamedFactory<KeyExchange>> getKeyExchangeFactories()
    {
        return keyExchangeFactories;
    }
    
    public KeyExchanger getKeyExchanger()
    {
        return kexer;
    }
    
    /**
     * Retrieve the list of named factories for <code>MAC</code>.
     * 
     * @return a list of named <code>Mac</code> factories, never {@code null}
     */
    public List<NamedFactory<MAC>> getMACFactories()
    {
        return macFactories;
    }
    
    /**
     * Retrieve the {@link PRNG} factory to be used.
     * 
     * @return the {@link PRNG} factory, never {@code null}
     */
    public NamedFactory<PRNG> getPRNGFactory()
    {
        return prngFactory;
    }
    
    /**
     * Retrieve the list of named factories for {@link Signature}
     * 
     * @return a list of named {@link Signature} factories, never {@code null}
     */
    public List<NamedFactory<Signature>> getSignatureFactories()
    {
        return signatureFactories;
    }
    
    public String getVersion()
    {
        return version;
    }
    
    public void setCipherFactories(List<NamedFactory<Cipher>> cipherFactories)
    {
        this.cipherFactories = cipherFactories;
    }
    
    public void setCipherFactories(NamedFactory<Cipher>... cipherFactories)
    {
        setCipherFactories(Arrays.<NamedFactory<Cipher>> asList(cipherFactories));
    }
    
    public void setCompressionFactories(List<NamedFactory<Compression>> compressionFactories)
    {
        this.compressionFactories = compressionFactories;
    }
    
    public void setCompressionFactories(NamedFactory<Compression>... compressionFactories)
    {
        setCompressionFactories(Arrays.<NamedFactory<Compression>> asList(compressionFactories));
    }
    
    public void setDecoder(Decoder decoder)
    {
        this.decoder = decoder;
    }
    
    public void setEncoder(Encoder encoder)
    {
        this.encoder = encoder;
    }
    
    public void setFileKeyProviderFactories(List<NamedFactory<FileKeyProvider>> fileKeyProviderFactories)
    {
        this.fileKeyProviderFactories = fileKeyProviderFactories;
    }
    
    public void setFileKeyProviderFactories(NamedFactory<FileKeyProvider>... fileKeyProviderFactories)
    {
        setFileKeyProviderFactories(Arrays.<NamedFactory<FileKeyProvider>> asList(fileKeyProviderFactories));
    }
    
    public void setKeyExchangeFactories(List<NamedFactory<KeyExchange>> keyExchangeFactories)
    {
        this.keyExchangeFactories = keyExchangeFactories;
    }
    
    public void setKeyExchangeFactories(NamedFactory<KeyExchange>... keyExchangeFactories)
    {
        setKeyExchangeFactories(Arrays.<NamedFactory<KeyExchange>> asList(keyExchangeFactories));
    }
    
    public void setKeyExchanger(KeyExchanger kexer)
    {
        this.kexer = kexer;
    }
    
    public void setMACFactories(List<NamedFactory<MAC>> macFactories)
    {
        this.macFactories = macFactories;
    }
    
    public void setMACFactories(NamedFactory<MAC>... macFactories)
    {
        setMACFactories(Arrays.<NamedFactory<MAC>> asList(macFactories));
    }
    
    public void setPRNGFactory(NamedFactory<PRNG> prngFactory)
    {
        this.prngFactory = prngFactory;
    }
    
    public void setSignatureFactories(List<NamedFactory<Signature>> signatureFactories)
    {
        this.signatureFactories = signatureFactories;
    }
    
    public void setSignatureFactories(NamedFactory<Signature>... signatureFactories)
    {
        setSignatureFactories(Arrays.<NamedFactory<Signature>> asList(signatureFactories));
    }
    
    public void setVersion(String version)
    {
        this.version = version;
    }
    
}
