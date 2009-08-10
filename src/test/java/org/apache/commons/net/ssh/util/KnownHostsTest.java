package org.apache.commons.net.ssh.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class KnownHostsTest
{
    
    private static final String schmizzDotNetEntry =
            "schmizz.net,69.163.155.180 ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEA6P9Hlwdahh250jGZYKg2snRq2j2lFJVdKSHyxqbJiVy9VX9gTkN3K2MD48qyrYLYOyGs3vTttyUk+cK++JMzURWsrP4piby7LpeOT+3Iq8CQNj4gXZdcH9w15Vuk2qS11at6IsQPVHpKD9HGg9//EFUccI/4w06k4XXLm/IxOGUwj6I2AeWmEOL3aDi+fe07TTosSdLUD6INtR0cyKsg0zC7Da24ixoShT8Oy3x2MpR7CY3PQ1pUVmvPkr79VeA+4qV9F1JM09WdboAMZgWQZ+XrbtuBlGsyhpUHSCQOya+kOJ+bYryS+U7A+6nmTW3C9FX4FgFqTF89UHOC7V0zZQ==";
    
    private static final String localhostEntry =
            "|1|SoWkWwK64ZF3OgKhe08AuPsnk2w=|cv5LDx+Ak5ffh8P9MXYl7RW+Zh4= ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAu64GJcCkdtckPGt8uKTyhG1ShT1Np1kh10eE49imQ4Nh9Y/IrSPzDtYUAazQ88ABc2NffuOKkdn2qtUwZ1ulfcdNfN3oTim3BiVHqa041pKG0L+onQe8Bo+CaG5KBLy/C24eNGM9EcfQvDQOnq1eD3lnR/l8fFckldzjfxZgar0yT9Bb3pwp50oN+1wSEINJEHOgMIW8kZBQmyNr/B+b7yX+Y1s1vuYIP/i4WimCVmkdi9G87Ga8w7GxKalRD2QOG6Xms2YWRQDN6M/MOn4tda3EKolbWkctEWcQf/PcVJffTH4Wv5f0RjVyrQv4ha4FZcNAv6RkRd9WkiCsiTKioQ==";
    
    private static final String content = schmizzDotNetEntry + "\n" + localhostEntry + "\n";
    
    private KnownHosts kh;
    
    @Before
    public void setUp() throws IOException
    {
        kh = new KnownHosts(new ByteArrayInputStream(content.getBytes()));
    }
    
    public void testNotOK()
    {
    }
    
    @Test
    public void testOK() throws UnknownHostException
    {
        
    }
    
    @Test
    public void testToString()
    {
        Assert.assertEquals(kh.getEntries()[0].toString(), schmizzDotNetEntry);
        Assert.assertEquals(kh.getEntries()[1].toString(), localhostEntry);
    }
    
}
