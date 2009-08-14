package org.apache.commons.net.ssh.util;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

public class KeyUtil
{
    
    /**
     * Creates an RSA private key.
     * 
     * @param modulus
     *            modulus (hex)
     * @param exponent
     *            private exponent (hex)
     * @return the generated private key
     * @throws GeneralSecurityException
     */
    public static PrivateKey newRSAPrivateKey(String modulus, String exponent) throws GeneralSecurityException
    {
        return SecurityUtils.getKeyFactory("RSA").generatePrivate(new RSAPrivateKeySpec //
                                                                  (new BigInteger(modulus, 16),
                                                                   new BigInteger(exponent, 16)));
    }
    
    /**
     * Creates an RSA public key.
     * 
     * @param modulus
     *            modulus (hex)
     * @param exponent
     *            public exponent (hex)
     * @return the generated public key
     * @throws GeneralSecurityException
     */
    public static PublicKey newRSAPublicKey(String modulus, String exponent) throws GeneralSecurityException
    {
        return SecurityUtils.getKeyFactory("RSA").generatePublic(new RSAPublicKeySpec //
                                                                 (new BigInteger(modulus, 16), new BigInteger(exponent,
                                                                                                              16)));
    }
    
}
