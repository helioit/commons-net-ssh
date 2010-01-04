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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.net.SocketClient;
import org.apache.commons.net.ssh.Factory.Named;
import org.apache.commons.net.ssh.cipher.AES128CBC;
import org.apache.commons.net.ssh.cipher.AES128CTR;
import org.apache.commons.net.ssh.cipher.AES192CBC;
import org.apache.commons.net.ssh.cipher.AES192CTR;
import org.apache.commons.net.ssh.cipher.AES256CBC;
import org.apache.commons.net.ssh.cipher.AES256CTR;
import org.apache.commons.net.ssh.cipher.BlowfishCBC;
import org.apache.commons.net.ssh.cipher.Cipher;
import org.apache.commons.net.ssh.cipher.TripleDESCBC;
import org.apache.commons.net.ssh.compression.DelayedZlibCompression;
import org.apache.commons.net.ssh.compression.NoneCompression;
import org.apache.commons.net.ssh.compression.ZlibCompression;
import org.apache.commons.net.ssh.connection.ConnectListener;
import org.apache.commons.net.ssh.connection.Connection;
import org.apache.commons.net.ssh.connection.ConnectionException;
import org.apache.commons.net.ssh.connection.ConnectionProtocol;
import org.apache.commons.net.ssh.connection.LocalPortForwarder;
import org.apache.commons.net.ssh.connection.RemotePortForwarder;
import org.apache.commons.net.ssh.connection.Session;
import org.apache.commons.net.ssh.connection.SessionChannel;
import org.apache.commons.net.ssh.connection.X11Forwarder;
import org.apache.commons.net.ssh.connection.RemotePortForwarder.ForwardedTCPIPChannel;
import org.apache.commons.net.ssh.connection.X11Forwarder.X11Channel;
import org.apache.commons.net.ssh.kex.DHG1;
import org.apache.commons.net.ssh.kex.DHG14;
import org.apache.commons.net.ssh.keyprovider.FileKeyProvider;
import org.apache.commons.net.ssh.keyprovider.KeyPairWrapper;
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
import org.apache.commons.net.ssh.scp.SCPFileTransfer;
import org.apache.commons.net.ssh.sftp.SFTPClient;
import org.apache.commons.net.ssh.sftp.StatefulSFTPClient;
import org.apache.commons.net.ssh.signature.SignatureDSA;
import org.apache.commons.net.ssh.signature.SignatureRSA;
import org.apache.commons.net.ssh.transport.Transport;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.transport.TransportProtocol;
import org.apache.commons.net.ssh.userauth.AuthMethod;
import org.apache.commons.net.ssh.userauth.AuthPassword;
import org.apache.commons.net.ssh.userauth.AuthPublickey;
import org.apache.commons.net.ssh.userauth.UserAuth;
import org.apache.commons.net.ssh.userauth.UserAuthException;
import org.apache.commons.net.ssh.userauth.UserAuthProtocol;
import org.apache.commons.net.ssh.util.KnownHosts;
import org.apache.commons.net.ssh.util.PasswordFinder;
import org.apache.commons.net.ssh.util.SecurityUtils;
import org.apache.commons.net.ssh.util.Constants.DisconnectReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secure SHell client API.
 * <p>
 * Before connection is established, host key verification needs to be accounted for. This is done by
 * {@link #addHostKeyVerifier(HostKeyVerifier) specifying} one or more {@link HostKeyVerifier} objects. Database of
 * known hostname-key pairs in the OpenSSH {@code "known_hosts"} format can be {@link #loadKnownHosts(String) loaded}
 * for host key verification.
 * <p>
 * User authentication can be performed by any of the {@code auth*()} methods.
 * <p>
 * {@link #startSession()} caters to the most typical use case of starting a {@code session} channel and executing a
 * remote command, starting a subsystem, etc. If you wish to request X11 forwarding for some session, first
 * {@link #registerX11Forwarder(ConnectListener) register} a {@link ConnectListener} for {@code x11} channels.
 * <p>
 * {@link #newLocalPortForwarder Local} and {@link #getRemotePortForwarder() remote} port forwarding is possible. There
 * are also utility methods for easily creating {@link #newSCPFileTransfer SCP} and {@link #newSFTPClient() SFTP}
 * implementations.
 * <p>
 * <em>A simple example:</em>
 * <p>
 * 
 * <pre>
 * client = new SSHClient();
 * client.initUserKnownHosts();
 * client.connect(&quot;hostname&quot;);
 * try
 * {
 *     client.authPassword(&quot;username&quot;, &quot;password&quot;);
 *     client.startSession().exec(&quot;true&quot;);
 *     client.getConnection().join();
 * } finally
 * {
 *     client.disconnect();
 * }
 * </pre>
 * <p>
 * Where a password or passphrase is required, if you're extra-paranoid use the {@code char[]} based methods. The
 * {@code char[]} will be blanked out after use.
 */
public class SSHClient extends SocketClient implements SessionFactory
{
    
    /** Default port for SSH */
    public static final int DEFAULT_PORT = 22;
    
    /** Logger */
    protected static final Logger log = LoggerFactory.getLogger(SSHClient.class);
    
    /**
     * Creates a {@link Config} object that is initialized as follows. Items marked with an asterisk are added to the
     * config only if {@link BouncyCastle} is in the classpath.
     * <p>
     * <ul>
     * <li>{@link Config#setKeyExchangeFactories Key exchange}: {@link DHG14}*, {@link DHG1}</li>
     * <li>{@link Config#setCipherFactories Ciphers} [1]: {@link AES128CTR}, {@link AES192CTR}, {@link AES256CTR},
     * {@link AES128CBC}, {@link AES192CBC}, {@link AES256CBC}, {@link AES192CBC}, {@link TripleDESCBC},
     * {@link BlowfishCBC}</li>
     * <li>{@link Config#setMACFactories MAC}: {@link HMACSHA1}, {@link HMACSHA196}, {@link HMACMD5}, {@link HMACMD596}</li>
     * <li>{@link Config#setCompressionFactories Compression}: {@link NoneCompression}</li>
     * <li>{@link Config#setSignatureFactories Signature}: {@link SignatureRSA}, {@link SignatureDSA}</li>
     * <li>{@link Config#setRandomFactory PRNG}: {@link BouncyCastleRandom}* or {@link JCERandom}</li>
     * <li>{@link Config#setFileKeyProviderFactories Key file support}: {@link PKCS8KeyFile}*, {@link OpenSSHKeyFile}*</li>
     * <li>{@link Config#setVersion Client version}: {@code "NET_3_0"}</li>
     * </ul>
     * <p>
     * [1] It is worth noting that Sun's JRE does not have the unlimited cryptography extension enabled by default. This
     * prevents using the ciphers of strength greater than 128.
     * 
     * @return initialized {@link Config}
     */
    @SuppressWarnings("unchecked")
    public static Config getDefaultConfig()
    {
        Config conf = new Config();
        conf.setVersion("NET_3_0");
        
        if (SecurityUtils.isBouncyCastleRegistered())
        {
            
            conf.setKeyExchangeFactories(new DHG14.Factory(), //
                    new DHG1.Factory());
            
            conf.setRandomFactory(new SingletonRandomFactory(new BouncyCastleRandom.Factory()));
            
            conf.setFileKeyProviderFactories(new PKCS8KeyFile.Factory(), //
                    new OpenSSHKeyFile.Factory());
            
        } else
        {
            conf.setKeyExchangeFactories(new DHG1.Factory());
            conf.setRandomFactory(new SingletonRandomFactory(new JCERandom.Factory()));
        }
        
        List<Named<Cipher>> avail = new LinkedList<Named<Cipher>>(Arrays.<Named<Cipher>> asList(
                new AES128CTR.Factory(), //
                new AES192CTR.Factory(), //
                new AES256CTR.Factory(), //
                new AES128CBC.Factory(), //
                new AES192CBC.Factory(), // 
                new AES256CBC.Factory(), //
                new TripleDESCBC.Factory(), //
                new BlowfishCBC.Factory()));
        
        { /*
           * @see https://issues.apache.org/jira/browse/SSHD-24:
           * "AES256 and AES192 requires unlimited cryptography extension"
           */
            for (Iterator<Named<Cipher>> i = avail.iterator(); i.hasNext();)
            {
                final Named<Cipher> f = i.next();
                try
                {
                    final Cipher c = f.create();
                    final byte[] key = new byte[c.getBlockSize()];
                    final byte[] iv = new byte[c.getIVSize()];
                    c.init(Cipher.Mode.Encrypt, key, iv);
                } catch (Exception e)
                {
                    log.warn("Disabling cipher: {}", f.getName());
                    i.remove();
                }
            }
        }
        
        conf.setCipherFactories(avail);
        
        conf.setCompressionFactories(new NoneCompression.Factory());
        
        conf.setMACFactories(new HMACSHA1.Factory(), new HMACSHA196.Factory(), new HMACMD5.Factory(),
                new HMACMD596.Factory());
        
        conf.setSignatureFactories(new SignatureRSA.Factory(), new SignatureDSA.Factory());
        
        return conf;
    }
    
    /** Transport layer */
    protected final Transport trans;
    
    /** {@code ssh-userauth} service */
    protected final UserAuth auth;
    
    /** {@code ssh-connection} service */
    protected final ConnectionProtocol conn;
    
    protected String hostname;
    
    /** Default constructor. Initializes this object using {@link #getDefaultConfig()}. */
    public SSHClient()
    {
        this(getDefaultConfig());
    }
    
    /** Constructor that allows specifying a {@code config} to be used. */
    public SSHClient(Config config)
    {
        setDefaultPort(DEFAULT_PORT);
        this.trans = new TransportProtocol(config);
        this.auth = new UserAuthProtocol(trans);
        this.conn = new ConnectionProtocol(trans);
    }
    
    /**
     * Add a {@link HostKeyVerifier} which will be invoked for verifying host key during connection establishment and
     * future key exchanges.
     */
    public void addHostKeyVerifier(HostKeyVerifier hostKeyVerifier)
    {
        trans.addHostKeyVerifier(hostKeyVerifier);
    }
    
    /**
     * Add a {@link HostKeyVerifier} that will verify any host with given hostname and a host key that has the given
     * {@code fingerprint}, e.g. {@code "4b:69:6c:72:6f:79:20:77:61:73:20:68:65:72:65:21"}
     * 
     * @param hostname
     *            the hostname
     * @param fingerprint
     *            the expected fingerprint in colon-delimited format (16 octets in hex delimited by a colon)
     * 
     * @see SecurityUtils#getFingerprint
     */
    public void addHostKeyVerifier(final String hostname, final String fingerprint)
    {
        addHostKeyVerifier(new HostKeyVerifier()
        {
            public boolean verify(String hostname, PublicKey key)
            {
                return hostname.equals(hostname) && SecurityUtils.getFingerprint(key).equals(fingerprint);
            }
        });
    }
    
    /**
     * Authenticate {@code username} using the supplied {@code methods}.
     * 
     * @param username
     *            user to authenticate
     * @param methods
     *            one or more authentication methods
     * @throws UserAuthException
     * @throws TransportException
     */
    public void auth(String username, AuthMethod... methods) throws UserAuthException, TransportException
    {
        assert isConnected();
        auth(username, Arrays.<AuthMethod> asList(methods));
    }
    
    /**
     * Authenticate {@code username} using the supplied {@code methods}.
     * 
     * @param username
     *            user to authenticate
     * @param methods
     *            one or more authentication methods
     * @throws UserAuthException
     * @throws TransportException
     */
    public void auth(String username, Iterable<AuthMethod> methods) throws UserAuthException, TransportException
    {
        assert isConnected();
        auth.authenticate(username, conn, methods);
    }
    
    /**
     * Authenticate {@code username} using the {@code "password"} authentication method. The {@code password} array is
     * blanked out after use.
     * 
     * @param username
     *            user to authenticate
     * @param password
     *            the password to use for authentication
     * @throws UserAuthException
     * @throws TransportException
     */
    public void authPassword(String username, char[] password) throws UserAuthException, TransportException
    {
        authPassword(username, PasswordFinder.Util.createOneOff(password));
    }
    
    /**
     * Authenticate {@code username} using the {@code "password"} authentication method.
     * 
     * @param username
     *            user to authenticate
     * @param pfinder
     *            the {@link PasswordFinder} to use for authentication
     * @throws UserAuthException
     * @throws TransportException
     */
    public void authPassword(String username, PasswordFinder pfinder) throws UserAuthException, TransportException
    {
        auth(username, new AuthPassword(pfinder));
    }
    
    /**
     * Authenticate {@code username} using the {@code "password"} authentication method.
     * 
     * @param username
     *            user to authenticate
     * @param password
     *            the password to use for authentication
     * @throws UserAuthException
     * @throws TransportException
     */
    public void authPassword(String username, String password) throws UserAuthException, TransportException
    {
        authPassword(username, password.toCharArray());
    }
    
    /**
     * Authenticate {@code username} using the {@code "publickey"} authentication method, with keys from some commons
     * locations on the file system. This method relies on {@code ~/.ssh/id_rsa} and {@code ~/.ssh/id_dsa}.
     * <p>
     * This method does not provide a way to specify a passphrase.
     * 
     * @param username
     *            user to authenticate
     * @throws UserAuthException
     * @throws TransportException
     */
    public void authPublickey(String username) throws UserAuthException, TransportException
    {
        String base = System.getProperty("user.home") + File.separator + ".ssh" + File.separator;
        authPublickey(username, base + "id_rsa", base + "id_dsa");
    }
    
    /**
     * Authenticate {@code username} using the {@code "publickey"} authentication method.
     * <p>
     * {@link KeyProvider} instances can be created using any of the of the {@code loadKeys(*)} methods provided in this
     * class. In case multiple {@code keyProviders} are specified; authentication is attempted in order as long as the
     * {@code "publickey"} authentication method is available.
     * 
     * @param username
     *            user to authenticate
     * @param keyProviders
     *            one or more {@link KeyProvider} instances
     * @throws UserAuthException
     * @throws TransportException
     */
    public void authPublickey(String username, Iterable<KeyProvider> keyProviders) throws UserAuthException,
            TransportException
    {
        List<AuthMethod> am = new LinkedList<AuthMethod>();
        for (KeyProvider kp : keyProviders)
            am.add(new AuthPublickey(kp));
        auth(username, am);
    }
    
    /**
     * Authenticate {@code username} using the {@code "publickey"} authentication method.
     * <p>
     * {@link KeyProvider} instances can be created using any of the {@code loadKeys(*)} methods provided in this class.
     * In case multiple {@code keyProviders} are specified; authentication is attempted in order as long as the {@code
     * "publickey"} authentication method is available.
     * 
     * @param username
     *            user to authenticate
     * @param keyProviders
     *            one or more {@link KeyProvider} instances
     * @throws UserAuthException
     * @throws TransportException
     */
    public void authPublickey(String username, KeyProvider... keyProviders) throws UserAuthException,
            TransportException
    {
        authPublickey(username, Arrays.<KeyProvider> asList(keyProviders));
    }
    
    /**
     * Authenticate {@code username} using the {@code "publickey"} authentication method, with keys from one or more
     * {@code locations} in the file system.
     * <p>
     * In case multiple {@code locations} are specified; authentication is attempted in order as long as the {@code
     * "publickey"} authentication method is available. If there is an error loading keys from any of them (e.g. file
     * could not be read, file format not recognized) that key file it is ignored.
     * <p>
     * This method does not provide a way to specify a passphrase.
     * 
     * @param username
     *            user to authenticate
     * @param locations
     *            one or more locations in the file system containing the private key
     * @throws UserAuthException
     * @throws TransportException
     */
    public void authPublickey(String username, String... locations) throws UserAuthException, TransportException
    {
        List<KeyProvider> keyProviders = new LinkedList<KeyProvider>();
        for (String loc : locations)
            try
            {
                log.debug("Attempting to load key from: {}", loc);
                keyProviders.add(loadKeys(loc));
            } catch (IOException logged)
            {
                log.warn("Could not load keys due to: {}", logged);
            }
        authPublickey(username, keyProviders);
    }
    
    /**
     * Disconnects from the connected SSH server. {@code SSHClient} objects are not reusable therefore it is incorrect
     * to attempt connection after this method has been called.
     * <p>
     * This method should be called from a {@code finally} construct after connection is established; so that proper
     * cleanup is done and the thread spawned by the transport layer for dealing with incoming packets is stopped.
     */
    @Override
    public void disconnect() throws IOException
    {
        assert isConnected();
        trans.disconnect();
        super.disconnect();
        assert !isConnected();
    }
    
    /** Returns the {@link Config} for this client. */
    public Config getConfig()
    {
        return trans.getConfig();
    }
    
    /** Returns the associated {@link Connection} instance. */
    public Connection getConnection()
    {
        return conn;
    }
    
    /**
     * Returns a {@link RemotePortForwarder} that allows requesting remote forwarding over this connection.
     */
    public RemotePortForwarder getRemotePortForwarder()
    {
        synchronized (conn)
        {
            RemotePortForwarder rpf = (RemotePortForwarder) conn.get(ForwardedTCPIPChannel.TYPE);
            if (rpf == null)
                conn.attach(rpf = new RemotePortForwarder(conn));
            return rpf;
        }
    }
    
    /** Returns the associated {@link Transport} instance. */
    public Transport getTransport()
    {
        return trans;
    }
    
    /**
     * Returns the associated {@link UserAuth} instance. This allows access to information like the
     * {@link UserAuth#getBanner() authentication banner}, whether authentication was at least
     * {@link UserAuth#hadPartialSuccess() partially successful}, and any {@link UserAuth#getSavedExceptions() saved
     * exceptions} that were ignored because there were more authentication methods that could be tried.
     */
    public UserAuth getUserAuth()
    {
        return auth;
    }
    
    /** Whether authenticated. */
    public boolean isAuthenticated()
    {
        return trans.isAuthenticated();
    }
    
    /** Whether connected. */
    @Override
    public boolean isConnected()
    {
        return super.isConnected() && trans.isRunning();
    }
    
    /**
     * Creates a {@link KeyProvider} from supplied {@link KeyPair}.
     * 
     * @param kp
     *            the key pair
     * @return the key provider ready for use in authentication
     */
    public KeyProvider loadKeys(KeyPair kp)
    {
        return new KeyPairWrapper(kp);
    }
    
    /**
     * Returns a {@link KeyProvider} instance created from a location on the file system where an <em>unencrypted</em>
     * private key file (does not require a passphrase) can be found. Simply calls
     * {@link #loadKeys(String, PasswordFinder)} with the {@link PasswordFinder} argument as {@code null}.
     * 
     * @param location
     *            the location for the key file
     * @return the key provider ready for use in authentication
     * @throws SSHException
     *             if there was no suitable key provider available for the file format; typically because BouncyCastle
     *             is not in the classpath
     * @throws IOException
     *             if the key file format is not known, if the file could not be read, etc.
     */
    public KeyProvider loadKeys(String location) throws IOException
    {
        return loadKeys(location, (PasswordFinder) null);
    }
    
    /**
     * Utility function for createing a {@link KeyProvider} instance from given location on the file system. Creates a
     * one-off {@link PasswordFinder} using {@link PasswordFinder.Util#createOneOff(char[])}, and calls
     * {@link #loadKeys(String,PasswordFinder)}.
     * 
     * @param location
     *            location of the key file
     * @param passphrase
     *            passphrase as a char-array
     * @return the key provider ready for use in authentication
     * @throws SSHException
     *             if there was no suitable key provider available for the file format; typically because BouncyCastle
     *             is not in the classpath
     * @throws IOException
     *             if the key file format is not known, if the file could not be read, etc.
     */
    public KeyProvider loadKeys(String location, char[] passphrase) throws IOException
    {
        return loadKeys(location, PasswordFinder.Util.createOneOff(passphrase));
    }
    
    /**
     * Creates a {@link KeyProvider} instance from given location on the file system. Currently only PKCS8 format
     * private key files are supported (OpenSSH uses this format).
     * <p>
     * 
     * @param location
     *            the location of the key file
     * @param passwordFinder
     *            the {@link PasswordFinder} that can supply the passphrase for decryption (may be {@code null} in case
     *            keyfile is not encrypted)
     * @return the key provider ready for use in authentication
     * @throws SSHException
     *             if there was no suitable key provider available for the file format; typically because BouncyCastle
     *             is not in the classpath
     * @throws IOException
     *             if the key file format is not known, if the file could not be read, etc.
     */
    public KeyProvider loadKeys(String location, PasswordFinder passwordFinder) throws IOException
    {
        File loc = new File(location);
        FileKeyProvider.Format format = SecurityUtils.detectKeyFileFormat(loc);
        FileKeyProvider fkp = Factory.Named.Util.create(trans.getConfig().getFileKeyProviderFactories(), format
                .toString());
        if (fkp == null)
            throw new SSHException("No provider available for " + format + " key file");
        fkp.init(loc, passwordFinder);
        return fkp;
    }
    
    /**
     * Convenience method for creating a {@link KeyProvider} instance from a {@code location} where an <i>encrypted</i>
     * key file is located. Calls {@link #loadKeys(String, char[])} with a character array created from the supplied
     * {@code passphrase} string.
     * 
     * @param location
     *            location of the key file
     * @param passphrase
     *            passphrase as a string
     * @return the key provider for use in authentication
     * @throws IOException
     *             if the key file format is not known, if the file could not be read etc.
     */
    public KeyProvider loadKeys(String location, String passphrase) throws IOException
    {
        return loadKeys(location, passphrase.toCharArray());
    }
    
    /**
     * Attempts loading the user's {@code known_hosts} file from the default location, i.e. {@code ~/.ssh/known_hosts}
     * and {@code ~/.ssh/known_hosts2} on most platforms. Adds the resulting {@link KnownHosts} object as a host key
     * verifier.
     * <p>
     * For finer control over which file is used, see {@link #loadKnownHosts(String)}.
     * 
     * @throws IOException
     *             if there is an error loading from <em>both</em> locations
     */
    public void loadKnownHosts() throws IOException
    {
        String homeDir = System.getProperty("user.home");
        boolean a = false, b = false;
        if (homeDir != null)
        {
            String kh = homeDir + File.separator + ".ssh" + File.separator + "known_hosts";
            try
            {
                loadKnownHosts(kh); // "~/.ssh/known_hosts"
            } catch (IOException ignored)
            {
                a = true;
            }
            try
            {
                loadKnownHosts(kh + "2"); // "~/.ssh/known_hosts2"
            } catch (IOException ignored)
            {
                b = true;
            }
        }
        if (a && b)
            throw new IOException("Could not load user known_hosts");
    }
    
    /**
     * Adds a {@link KnownHosts} object created from the specified location as a host key verifier.
     * 
     * @param location
     *            location for {@code known_hosts} file
     * @throws IOException
     *             if there is an error loading from any of these locations
     */
    public void loadKnownHosts(String location) throws IOException
    {
        addHostKeyVerifier(new KnownHosts(new File(location)));
    }
    
    /**
     * Create a {@link LocalPortForwarder} that will listen on {@code address} and forward incoming connections to the
     * server; which will further forward them to {@code host:port}.
     * <p>
     * The returned forwarder's {@link LocalPortForwarder#startListening() startListening()} method should be called to
     * actually start listening, this method just creates an instance.
     * 
     * @param address
     *            defines where the {@link LocalPortForwarder} listens
     * @param host
     *            hostname to which the server will forward
     * @param port
     *            the port at {@code hostname} to which the server wil forward
     * @return
     * @throws IOException
     */
    public LocalPortForwarder newLocalPortForwarder(SocketAddress address, String host, int port) throws IOException
    {
        return new LocalPortForwarder(conn, address, host, port);
    }
    
    /**
     * Register a {@code listener} for handling forwarded X11 channels. Without having done this, an incoming X11
     * forwarding will be summarily rejected.
     * <p>
     * It should be clarified that multiple listeners for X11 forwarding over a single SSH connection are not supported
     * (and don't make much sense). So a subsequent call to this method is only going to replace the registered {@code
     * listener}.
     * 
     * @param listener
     *            the {@link ConnectListener} that should be delegated the responsibility of handling forwarded
     *            {@link X11Channel} 's
     * @return an {@link X11Forwarder} that allows to {@link X11Forwarder#stop() stop acting} on X11 requests from
     *         server
     */
    public X11Forwarder registerX11Forwarder(ConnectListener listener)
    {
        X11Forwarder x11f = new X11Forwarder(conn, listener);
        conn.attach(x11f);
        return x11f;
    }
    
    /**
     * Instantiates a {@link SCPFileTransfer} implementation.
     */
    public SCPFileTransfer newSCPFileTransfer()
    {
        assert isConnected() && isAuthenticated();
        return new SCPFileTransfer(this);
    }
    
    /**
     * Instantiates {@link SFTPClient} implementation.
     * 
     * @throws IOException
     *             if there is an error starting the {@code sftp} subsystem
     * @see StatefulSFTPClient
     */
    public SFTPClient newSFTPClient() throws IOException
    {
        assert isConnected() && isAuthenticated();
        return new SFTPClient(this);
    }
    
    /**
     * Does key re-exchange.
     * 
     * @throws TransportException
     *             if an error occurs during key exchange
     */
    public void rekey() throws TransportException
    {
        doKex();
    }
    
    /**
     * Opens a {@code session} channel. The returned {@link Session} instance allows {@link Session#exec(String)
     * executing a remote command}, {@link Session#startSubsysytem(String) starting a subsystem}, or
     * {@link Session#startShell() starting a shell}.
     * 
     * @return the opened {@code session} channel
     * @throws ConnectionException
     * @throws TransportException
     * @see {@link Session}
     */
    public Session startSession() throws ConnectionException, TransportException
    {
        assert isConnected() && isAuthenticated();
        SessionChannel sess = new SessionChannel(conn);
        sess.open();
        assert sess.isOpen();
        return sess;
    }
    
    /**
     * Adds {@code zlib} compression to preferred compression algorithms. There is no guarantee that it will be
     * successfully negotiatied.
     * <p>
     * If the client is already connected renegotiation is done; otherwise this method simply returns (and compression
     * will be negotiated during connection establishment).
     * 
     * @throws ClassNotFoundException
     *             if {@code JZlib} is not in classpath
     * @throws TransportException
     *             if an error occurs during renegotiation
     */
    @SuppressWarnings("unchecked")
    public void useCompression() throws TransportException
    {
        trans.getConfig().setCompressionFactories(new DelayedZlibCompression.Factory(), //
                new ZlibCompression.Factory(), //
                new NoneCompression.Factory());
        if (isConnected())
            rekey();
    }
    
    /**
     * On connection establishment, also initialize the SSH transport via {@link Transport#init} and {@link #doKex()}.
     */
    @Override
    protected void _connectAction_() throws IOException
    {
        super._connectAction_();
        trans.init(new ConnInfo(hostname, _socket_));
        doKex();
    }
    
    @Override
    public void connect(String hostname, int port) throws SocketException, IOException
    {
        this.hostname = hostname;
        super.connect(hostname, port);
    }
    
    @Override
    public void connect(String hostname, int port, InetAddress localAddr, int localPort) throws SocketException,
            IOException
    {
        this.hostname = hostname;
        super.connect(hostname, port, localAddr, localPort);
    }
    
    /**
     * Do key exchange.
     * 
     * @throws TransportException
     *             if error during kex
     */
    protected void doKex() throws TransportException
    {
        assert trans.isRunning();
        
        long start = System.currentTimeMillis();
        
        try
        {
            trans.doKex();
        } catch (TransportException te)
        {
            trans.disconnect(DisconnectReason.KEY_EXCHANGE_FAILED);
            throw te;
        }
        
        log.info("Key exchange took {} seconds", (System.currentTimeMillis() - start) / 1000.0);
    }
    
}