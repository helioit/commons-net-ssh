package org.apache.commons.net.ssh.sftp;

import java.util.Set;

public enum FileMode {
    
    READ(0x00000001),
    WRITE(0x00000002),
    APPEND(0x00000004),
    CREAT(0x00000008),
    TRUNC(0x00000010),
    EXCL(0x00000020);
    
    private final int pflag;
    
    private FileMode(int pflag) {
        this.pflag = pflag;
    }
    
    public static int toMask(Set<FileMode> modes) {
        int mask = 0;
        for (FileMode m : modes)
            mask |= m.pflag;
        return mask;
    }
    
}
