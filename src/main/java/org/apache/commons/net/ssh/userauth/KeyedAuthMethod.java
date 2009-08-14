package org.apache.commons.net.ssh.userauth;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.apache.commons.net.ssh.Factory;
import org.apache.commons.net.ssh.keyprovider.KeyProvider;
import org.apache.commons.net.ssh.signature.Signature;
import org.apache.commons.net.ssh.util.Buffer;
import org.apache.commons.net.ssh.util.Constants.KeyType;

/**
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public abstract class KeyedAuthMethod extends AbstractAuthMethod
{
    protected KeyProvider kProv;
    
    public KeyedAuthMethod(String name, KeyProvider kProv)
    {
        super(name);
        this.kProv = kProv;
    }
    
    protected Buffer putPubKey(Buffer reqBuf) throws UserAuthException
    {
        PublicKey key;
        try {
            key = kProv.getPublic();
        } catch (IOException ioe) {
            throw new UserAuthException("Problem getting public key", ioe);
        }
        
        // public key as 2 strings: [ key type | key blob ]
        reqBuf.putString(KeyType.fromKey(key).toString()) //
              .putString(new Buffer().putPublicKey(key).getCompactData());
        
        return reqBuf;
    }
    
    protected Buffer putSig(Buffer reqBuf) throws UserAuthException
    {
        PrivateKey key;
        try {
            key = kProv.getPrivate();
        } catch (IOException ioe) {
            throw new UserAuthException("Problem getting private key", ioe);
        }
        
        String kt = KeyType.fromKey(key).toString();
        Signature sigger = Factory.Named.Util.create(params.getTransport().getConfig().getSignatureFactories(), kt);
        if (sigger == null)
            throw new UserAuthException("Could not create signature instance for " + kt + " key");
        
        sigger.init(null, key);
        sigger.update(new Buffer().putString(params.getTransport().getKeyExchanger().getSessionID()) //
                                  .putBuffer(reqBuf) // & rest of the data for sig
                                  .getCompactData());
        reqBuf.putSignature(kt, sigger.sign());
        return reqBuf;
    }
    
}
