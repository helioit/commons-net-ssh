package org.apache.commons.net.ssh.xfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFileTransfer
{
    
    /** Logger */
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final ModeGetter defaultModeGetter = new DefaultModeGetter();
    public static final ModeSetter defaultModeSetter = new DefaultModeSetter();
    
    private volatile ModeGetter modeGetter = defaultModeGetter;
    private volatile ModeSetter modeSetter = defaultModeSetter;
    
    public void setModeGetter(ModeGetter modeGetter)
    {
        this.modeGetter = (modeGetter == null) ? defaultModeGetter : modeGetter;
    }
    
    public ModeGetter getModeGetter()
    {
        return this.modeGetter;
    }
    
    public void setModeSetter(ModeSetter modeSetter)
    {
        this.modeSetter = (modeSetter == null) ? defaultModeSetter : modeSetter;
    }
    
    public ModeSetter getModeSetter()
    {
        return this.modeSetter;
    }
    
}
