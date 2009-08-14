package org.apache.commons.net.ssh.util;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

public class KeyUtil
{
    
    /**
     * Creates a DSA private key.
     */
    public static PrivateKey newDSAPrivateKey(String x, String p, String q, String g) throws GeneralSecurityException
    {
        return SecurityUtils.getKeyFactory("DSA").generatePrivate(new DSAPrivateKeySpec //
                                                                  (new BigInteger(x, 16), //
                                                                   new BigInteger(p, 16), //
                                                                   new BigInteger(q, 16), // 
                                                                   new BigInteger(g, 16)));
    }
    
    /**
     * Creates a DSA public key.
     */
    public static PublicKey newDSAPublicKey(String y, String p, String q, String g) throws GeneralSecurityException
    {
        return SecurityUtils.getKeyFactory("DSA").generatePublic(new DSAPublicKeySpec //
                                                                 (new BigInteger(y, 16), //
                                                                  new BigInteger(p, 16), //
                                                                  new BigInteger(q, 16), // 
                                                                  new BigInteger(g, 16)));
    }
    
    /**
     * Creates an RSA private key.
     */
    public static PrivateKey newRSAPrivateKey(String modulus, String exponent) throws GeneralSecurityException
    {
        return SecurityUtils.getKeyFactory("RSA").generatePrivate(new RSAPrivateKeySpec //
                                                                  (new BigInteger(modulus, 16), //
                                                                   new BigInteger(exponent, 16)));
    }
    
    /**
     * Creates an RSA public key.
     */
    public static PublicKey newRSAPublicKey(String modulus, String exponent) throws GeneralSecurityException
    {
        return SecurityUtils.getKeyFactory("RSA").generatePublic(new RSAPublicKeySpec //
                                                                 (new BigInteger(modulus, 16), new BigInteger(exponent,
                                                                                                              16)));
    }
    
}
