package org.apache.commons.net.ssh.connection;

/**
 * Various signals that may be sent or received. The signals are from POSIX and simply miss the {@code "SIG_"}
 * prefix.
 */
public enum Signal
{
    
    ABRT("ABRT"), ALRM("ALRM"), FPE("FPE"), HUP("HUP"), ILL("ILL"), INT("INT"), KILL("KILL"), PIPE("PIPE"), QUIT(
            "QUIT"), SEGV("SEGV"), TERM("TERM"), USR1("USR1"), USR2("USR2"), UNKNOWN("UNKNOWN");
    
    /**
     * Create from the string representation used when the signal is received as part of an SSH packet.
     * 
     * @param name
     *            name of the signal as received
     * @return the enum constant inferred
     */
    public static Signal fromString(String name)
    {
        for (Signal sig : Signal.values())
            if (sig.name.equals(name))
                return sig;
        return UNKNOWN;
    }
    
    private final String name;
    
    private Signal(String name)
    {
        this.name = name;
    }
    
    @Override
    public String toString()
    {
        return name;
    }
    
}