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

import static org.apache.commons.net.ssh.util.Constants.DEFAULT_PORT;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.net.SocketClient;
import org.apache.commons.net.ssh.cipher.AES128CBC;
import org.apache.commons.net.ssh.cipher.AES128CTR;
import org.apache.commons.net.ssh.cipher.AES192CBC;
import org.apache.commons.net.ssh.cipher.AES192CTR;
import org.apache.commons.net.ssh.cipher.AES256CBC;
import org.apache.commons.net.ssh.cipher.AES256CTR;
import org.apache.commons.net.ssh.cipher.BlowfishCBC;
import org.apache.commons.net.ssh.cipher.Cipher;
import org.apache.commons.net.ssh.cipher.TripleDESCBC;
import org.apache.commons.net.ssh.compression.CompressionNone;
import org.apache.commons.net.ssh.connection.ConnectionException;
import org.apache.commons.net.ssh.connection.ConnectionProtocol;
import org.apache.commons.net.ssh.connection.ConnectionService;
import org.apache.commons.net.ssh.connection.LocalPortForwarder;
import org.apache.commons.net.ssh.connection.RemotePortForwarder;
import org.apache.commons.net.ssh.connection.Session;
import org.apache.commons.net.ssh.connection.SessionChannel;
import org.apache.commons.net.ssh.kex.DHG1;
import org.apache.commons.net.ssh.kex.DHG14;
import org.apache.commons.net.ssh.keyprovider.FileKeyProvider;
import org.apache.commons.net.ssh.keyprovider.KeyProvider;
import org.apache.commons.net.ssh.keyprovider.OpenSSHKeyFile;
import org.apache.commons.net.ssh.keyprovider.PKCS8KeyFile;
import org.apache.commons.net.ssh.mac.HMACMD5;
import org.apache.commons.net.ssh.mac.HMACMD596;
import org.apache.commons.net.ssh.mac.HMACSHA1;
import org.apache.commons.net.ssh.mac.HMACSHA196;
import org.apache.commons.net.ssh.random.BouncyCastleRandom;
import org.apache.commons.net.ssh.random.JCERandom;
import org.apache.commons.net.ssh.random.SingletonRandomFactory;
import org.apache.commons.net.ssh.signature.SignatureDSA;
import org.apache.commons.net.ssh.signature.SignatureRSA;
import org.apache.commons.net.ssh.transport.Transport;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.transport.TransportProtocol;
import org.apache.commons.net.ssh.userauth.AuthMethod;
import org.apache.commons.net.ssh.userauth.AuthPassword;
import org.apache.commons.net.ssh.userauth.AuthPublickey;
import org.apache.commons.net.ssh.userauth.UserAuthException;
import org.apache.commons.net.ssh.userauth.UserAuthProtocol;
import org.apache.commons.net.ssh.userauth.UserAuthService;
import org.apache.commons.net.ssh.util.KnownHosts;
import org.apache.commons.net.ssh.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secure Shell client API.
 * <p>
 * The default constructor initializes {@code SSHClient} using {@link #getConfigBuilder()}.
 * Optionally, {@code SSHClient} may be constructed with a {@link Config} instance that has been
 * initialized with implementations of the requisite algorithms.
 * <p>
 * Before connection is established, host key verification needs to be accounted for. This is done
 * by specifying one or more {@link HostKeyVerifier} objects. Database of known hostname-key pairs
 * in the OpenSSH {@code "known_hosts"} format can be loaded for host key verification.
 * <p>
 * Once connection has been established, user authentication can be completed by obtaining an
 * instance of {@link AuthBuilder}, or using any of the convenience methods provided in this class.
 * <p>
 * Example:
 * <p>
 * 
 * <pre>
 * client = new SSHClient();
 * client.initUserKnownHosts();
 * client.connect(&quot;localhost&quot;);
 * try {
 *     client.authPassword(&quot;username&quot;, &quot;password&quot;);
 *     // TODO
 * } finally {
 *     client.disconnect();
 * }
 * </pre>
 * 
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public class SSHClient extends SocketClient
{
    
    protected static final Logger log = LoggerFactory.getLogger(SSHClient.class);
    
    @SuppressWarnings("unchecked")
    public static Config getDefaultConfig()
    {
        Config conf = new Config();
        conf.setVersion("NET_3_0");
        
        if (SecurityUtils.isBouncyCastleRegistered()) {
            conf.setKeyExchangeFactories(new DHG14.Factory(), new DHG1.Factory());
            conf.setRandomFactory(new SingletonRandomFactory(new BouncyCastleRandom.Factory()));
            conf.setFileKeyProviderFactories(new PKCS8KeyFile.Factory(), new OpenSSHKeyFile.Factory());
        } else {
            conf.setKeyExchangeFactories(new DHG1.Factory());
            conf.setRandomFactory(new SingletonRandomFactory(new JCERandom.Factory()));
        }
        
        List<NamedFactory<Cipher>> avail = new LinkedList<NamedFactory<Cipher>> //
                (Arrays.<NamedFactory<Cipher>> asList(new AES128CTR.Factory(), new AES192CTR.Factory(),
                                                      new AES256CTR.Factory(), new AES128CBC.Factory(),
                                                      new AES192CBC.Factory(), new AES256CBC.Factory(),
                                                      new TripleDESCBC.Factory(), new BlowfishCBC.Factory()));
        
        { /*
           * @see https://issues.apache.org/jira/browse/SSHD-24:
           * "AES256 and AES192 requires unlimited cryptography extension"
           */
            for (Iterator<NamedFactory<Cipher>> i = avail.iterator(); i.hasNext();) {
                final NamedFactory<Cipher> f = i.next();
                try {
                    final Cipher c = f.create();
                    final byte[] key = new byte[c.getBlockSize()];
                    final byte[] iv = new byte[c.getIVSize()];
                    c.init(Cipher.Mode.Encrypt, key, iv);
                } catch (Exception e) {
                    log.warn("Disabling cipher: {}", f.getName());
                    i.remove();
                }
            }
        }
        
        conf.setCipherFactories(avail);
        conf.setCompressionFactories(new CompressionNone.Factory());
        conf.setMACFactories(new HMACSHA1.Factory(), new HMACSHA196.Factory(), new HMACMD5.Factory(),
                             new HMACMD596.Factory());
        conf.setSignatureFactories(new SignatureRSA.Factory(), new SignatureDSA.Factory());
        
        return conf;
    }
    
    protected final Transport trans;
    protected final ConnectionService conn;
    protected final UserAuthService auth;
    
    /**
     * Default constructor
     */
    public SSHClient()
    {
        this(getDefaultConfig());
    }
    
    /**
     * Constructor that allows specifying the {@link Config}
     * 
     * @param config
     */
    public SSHClient(Config config)
    {
        setDefaultPort(DEFAULT_PORT);
        trans = new TransportProtocol(config);
        auth = new UserAuthProtocol(trans);
        conn = new ConnectionProtocol(trans);
    }
    
    /**
     * Add a {@link HostKeyVerifier} whose {@link HostKeyVerifier#verify} method will be invoked for
     * verifying host key during connection establishment.
     * <p>
     * Needless to say, this is only relevant before connection is established.
     * 
     * @param hostKeyVerifier
     */
    public void addHostKeyVerifier(HostKeyVerifier hostKeyVerifier)
    {
        trans.addHostKeyVerifier(hostKeyVerifier);
    }
    
    public void addHostKeyVerifier(String fingerprint)
    {
        addHostKeyVerifier(HostKeyVerifier.Util.makeForFingerprint(fingerprint));
    }
    
    public void auth(String username, AuthMethod... methods) throws UserAuthException, TransportException
    {
        auth(username, Arrays.<AuthMethod> asList(methods));
    }
    
    public void auth(String username, Iterable<AuthMethod> methods) throws UserAuthException, TransportException
    {
        auth.authenticate(username, (Service) conn, methods);
    }
    
    /**
     * Attempts authentication using the {@code "password"} authentication method.
     * 
     * @param username
     *            the username to authenticate
     * @param password
     *            the password to use
     * @throws SSHException
     *             if an error occurs during the authentication process
     */
    public void authPassword(String username, char[] password) throws UserAuthException, TransportException
    {
        authPassword(username, PasswordFinder.Util.createOneOff(password));
    }
    
    /**
     * Attempts authentication using the {@code "password"} authentication method.
     * 
     * @param username
     *            the username to authenticate
     * @param pfinder
     *            the {@link PasswordFinder} to use
     * @throws SSHException
     *             if an error occurs during the authentication process
     */
    public void authPassword(String username, PasswordFinder pfinder) throws UserAuthException, TransportException
    {
        auth(username, new AuthPassword(pfinder));
    }
    
    public void authPublickey(String username) throws UserAuthException, TransportException
    {
        String base = System.getProperty("user.home") + File.separator + ".ssh" + File.separator;
        authPublickey(username, base + "id_rsa", base + "id_dsa");
    }
    
    /**
     * Attempts authentication using the {@code "publickey"} authentication method.
     * <p>
     * The {@code keyProvider} argument may be an instance of {@link FileKeyProvider} that is
     * created using any of the convenience methods provided in this class.
     * 
     * @param username
     *            the username to authenticate
     * @param keyProvider
     *            the {@link KeyProvider} for private key
     * @throws SSHException
     */
    public void authPublickey(String username, KeyProvider keyProvider) throws UserAuthException, TransportException
    {
        auth(username, new AuthPublickey(keyProvider));
    }
    
    public void authPublickey(String username, String... locations) throws UserAuthException, TransportException
    {
        List<AuthMethod> am = new LinkedList<AuthMethod>();
        for (String loc : locations)
            try {
                am.add(new AuthPublickey(loadKeyFile(loc)));
            } catch (IOException ignore) {
            }
        auth(username, am);
    }
    
    @Override
    public void disconnect() throws IOException
    {
        trans.disconnect();
        assert !trans.isRunning();
        super.disconnect();
    }
    
    public ConnectionService getConnectionService()
    {
        return conn;
    }
    
    public RemotePortForwarder getRemotePortForwarder()
    {
        return RemotePortForwarder.getInstance(conn);
    }
    
    /**
     * Returns the associated {@link Transport} instance.
     */
    public Transport getTransport()
    {
        return trans;
    }
    
    public UserAuthService getUserAuthService()
    {
        return auth;
    }
    
    /**
     * 
     * Creates {@link KnownHosts} object from the specified location.
     * 
     * @param loc
     *            location for {@code known_hosts} file
     * @throws IOException
     *             if there is an error loading from any of these locations
     */
    public void initKnownHosts(String loc) throws IOException
    {
        addHostKeyVerifier(new KnownHosts(loc));
    }
    
    /**
     * Attempts loading the user's {@code known_hosts} file from the default location, i.e. {@code
     * ~/.ssh/known_hosts} and {@code ~/.ssh/known_hosts2} on most platforms.
     * 
     * @throws IOException
     *             if there is an error loading from <b>both</b> locations
     */
    public void initUserKnownHosts() throws IOException
    {
        String homeDir = System.getProperty("user.home");
        boolean a = false, b = false;
        if (homeDir != null) {
            String kh = homeDir + File.separator + ".ssh" + File.separator + "known_hosts";
            try {
                initKnownHosts(kh); // "~/.ssh/known_hosts" 
            } catch (IOException ignored) {
                a = true;
            }
            try {
                initKnownHosts(kh + "2"); // "~/.ssh/known_hosts2" 
            } catch (IOException ignored) {
                b = true;
            }
        }
        if (a && b)
            throw new IOException("Could not load user known_hosts");
    }
    
    public boolean isAuthenticated()
    {
        return trans.isAuthenticated();
    }
    
    @Override
    public boolean isConnected()
    {
        return super.isConnected() && trans.isRunning();
    }
    
    /**
     * Convenience method for creating a {@link FileKeyProvider} instance from a location where the
     * key file is located.
     * 
     * @param location
     *            the location for the key file
     * @return the {@link FileKeyProvider} intialized with given location
     * @throws IOException
     *             if the key file format is not known, if the file could not be read, etc.
     */
    public FileKeyProvider loadKeyFile(String location) throws IOException
    {
        return loadKeyFile(location, new char[] {});
    }
    
    /**
     * Convenience method for creating a {@link FileKeyProvider} instance from a location where an
     * <i>encrypted</i> key file is located.
     * 
     * @param location
     *            the location of the key file
     * @param passphrase
     *            the passphrase for unlocking the key
     * @return the {@link FileKeyProvider} initialized with given location
     * @throws IOException
     *             if the key file format is not known, if the file could not be read etc.
     */
    public FileKeyProvider loadKeyFile(String location, char[] passphrase) throws IOException
    {
        return loadKeyFile(location, PasswordFinder.Util.createOneOff(passphrase));
    }
    
    /**
     * Convenience method for creating a {@link FileKeyProvider} instance from a location where an
     * <i>encrypted</i> key file is located.
     * 
     * @param location
     *            the location of the key file
     * @param pwdf
     *            the {@link PasswordFinder} that can supply the passphrase for decryption
     * @return the {@link FileKeyProvider} initialized with given location
     * @throws IOException
     *             if the key file format is not known, if the file could not be read, etc.
     */
    public FileKeyProvider loadKeyFile(String location, PasswordFinder pwdf) throws IOException
    {
        String format = SecurityUtils.detectKeyFileFormat(location);
        if (format.equals("unknown"))
            throw new IOException("Unknown key file format");
        FileKeyProvider fkp = NamedFactory.Utils.create(trans.getConfig().getFileKeyProviderFactories(), format);
        if (fkp != null)
            fkp.init(location, pwdf);
        else
            throw new SSHException("No provider available for " + format + " key file");
        return fkp;
    }
    
    public LocalPortForwarder newLocalPortForwarder(SocketAddress addr, String toHost, int toPort) throws IOException
    {
        return new LocalPortForwarder(conn, addr, toHost, toPort);
    }
    
    public Session startSession() throws ConnectionException, TransportException
    {
        SessionChannel sess = new SessionChannel(conn);
        sess.open();
        return sess;
    }
    
    /**
     * On connection establishment, also initialize the SSH transport via {@link Transport#open}
     */
    @Override
    protected void _connectAction_() throws IOException
    {
        super._connectAction_();
        trans.init(_socket_);
        trans.getKeyExchanger().startKex(true);
    }
    
    void forceRekey(boolean waitForDone) throws TransportException
    {
        trans.getKeyExchanger().startKex(waitForDone);
    }
    
}
