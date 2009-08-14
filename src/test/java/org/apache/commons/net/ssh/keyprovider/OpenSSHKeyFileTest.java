package org.apache.commons.net.ssh.keyprovider;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import org.apache.commons.net.ssh.util.PasswordFinder;
import org.apache.commons.net.ssh.util.Constants.KeyType;
import org.apache.sshd.common.util.SecurityUtils;
import org.junit.Before;
import org.junit.Test;

public class OpenSSHKeyFileTest
{
    
    private boolean readyToProvide;
    
    PasswordFinder pwdf = new PasswordFinder()
        {
            public char[] reqPassword(Resource resource)
            {
                if (!readyToProvide)
                    throw new AssertionError("Password requested too soon");
                
                return "test_passphrase".toCharArray();
            }
            
            public boolean shouldRetry(Resource resource)
            {
                return false;
            }
            
        };
    
    private final FileKeyProvider dsa = new OpenSSHKeyFile();
    
    @Before
    public void setUp() throws UnsupportedEncodingException, GeneralSecurityException
    {
        if (!SecurityUtils.isBouncyCastleRegistered())
            throw new AssertionError("bouncy castle needed");
        
        dsa.init(new File("src/test/resources/id_dsa"), pwdf);
    }
    
    @Test
    public void testKeys() throws IOException
    {
        dsa.getPublic(); // TODO - actually compare the key with what is expected
        readyToProvide = true;
        dsa.getPrivate(); // TODO - actually compare the key with what is expected
    }
    
    @Test
    public void testType() throws IOException
    {
        assertEquals(dsa.getType(), KeyType.DSA);
    }
    
}