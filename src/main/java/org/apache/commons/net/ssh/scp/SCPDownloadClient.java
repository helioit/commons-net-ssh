package org.apache.commons.net.ssh.scp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.net.ssh.SSHClient;
import org.apache.commons.net.ssh.connection.ConnectionException;
import org.apache.commons.net.ssh.transport.TransportException;
import org.apache.commons.net.ssh.util.IOUtils;

public class SCPDownloadClient extends SCPClient
{
    
    protected final ModeSetter modeSetter;
    
    protected boolean recursive = true;
    
    public SCPDownloadClient(SSHClient host)
    {
        this(host, null);
    }
    
    public SCPDownloadClient(SSHClient host, ModeSetter modeSetter)
    {
        super(host);
        this.modeSetter = modeSetter == null ? new DefaultModeSetter() : modeSetter;
    }
    
    public SCPDownloadClient recursive(boolean recursive)
    {
        this.recursive = recursive;
        return this;
    }
    
    protected File getTargetDirectory(File f, String dirname) throws IOException
    {
        if (f.exists())
            if (f.isDirectory())
                f = new File(f, dirname); //  <-- A
            else
                throw new IOException(f + " already exists as a file; remote end is sending directory");
        
        // else <-- B
        
        if (!f.exists())
            if (!f.mkdir())
                throw new IOException("Failed to create directory " + f);
        
        return f;
    }
    
    protected File getTargetFile(File f, String filename) throws IOException
    {
        if (f.exists()) {
            if (f.isDirectory())
                f = new File(f, filename);
        } else
            f.createNewFile();
        return f;
    }
    
    protected void init(String source) throws ConnectionException, TransportException
    {
        List<String> args = new LinkedList<String>();
        addArg(args, Arg.SOURCE);
        addArg(args, Arg.QUIET);
        if (recursive)
            addArg(args, Arg.RECURSIVE);
        if (modeSetter.shouldPreserveTimes())
            addArg(args, Arg.PRESERVE_MODES);
        addArg(args, source == null || source.equals("") ? "." : source);
        execSCPWith(args);
    }
    
    protected long parseLong(String longString, String valType) throws IOException
    {
        long val = 0;
        try {
            val = Long.parseLong(longString);
        } catch (NumberFormatException nfe) {
            throw new IOException("Could not parse " + valType + " from `" + longString + "`", nfe);
        }
        return val;
    }
    
    protected String parsePermissions(String cmd) throws IOException
    {
        // e.g. "C0644" -> "644"; "D0755" -> "755"
        if (cmd.length() != 5)
            throw new IOException("Could not parse permissions from `" + cmd + "`");
        return cmd.substring(2);
    }
    
    protected void prepare(File f, String perms, String tMsg) throws IOException
    {
        modeSetter.setPermissions(f, perms);
        
        if (tMsg != null && modeSetter.shouldPreserveTimes()) {
            String[] tMsgParts = tokenize(tMsg, 4); // e.g. T<mtime> 0 <atime> 0
            modeSetter.setLastModifiedTime(f, parseLong(tMsgParts[0].substring(1), "last modified time"));
            modeSetter.setLastAccessedTime(f, parseLong(tMsgParts[2], "last access time"));
        }
    }
    
    protected boolean process(String bufferedTMsg, String msg, File f) throws IOException
    {
        if (msg.length() < 1)
            throw new IOException("Could not parse message `" + msg + "`");
        
        switch (msg.charAt(0))
        {
        case 'T':
            signal("ACK: T");
            process(msg, readMessage(true), f);
            break;
        case 'C':
            processFile(msg, bufferedTMsg, f);
            break;
        case 'D':
            processDirectory(msg, bufferedTMsg, f);
            break;
        case 'E':
            return true;
        case (char) 1:
            addWarning(msg.substring(1));
            break;
        case (char) 2:
            throw new IOException("Remote SCP command returned error: " + msg.substring(1));
        default:
            String err = "Unrecognized message: `" + msg + "`";
            sendMessage((char) 2 + err);
            throw new IOException(err);
        }
        
        return false;
    }
    
    protected void processDirectory(String dMsg, String tMsg, File f) throws IOException
    {
        String[] dMsgParts = tokenize(dMsg, 3); // e.g. D0755 0 <dirname> 
        
        long length = parseLong(dMsgParts[1], "dir length");
        if (length != 0)
            throw new IOException("Remote SCP command sent strange directory length: " + length);
        
        f = getTargetDirectory(f, dMsgParts[2]);
        prepare(f, parsePermissions(dMsgParts[0]), tMsg);
        
        signal("ACK: D");
        
        while (!process(null, readMessage(), f))
            ;
        
        signal("ACK: E");
    }
    
    protected void processFile(String cMsg, String tMsg, File f) throws IOException
    {
        String[] cMsgParts = tokenize(cMsg, 3);
        
        long length = parseLong(cMsgParts[1], "length");
        
        f = getTargetFile(f, cMsgParts[2]);
        prepare(f, parsePermissions(cMsgParts[0]), tMsg);
        
        FileOutputStream fos = new FileOutputStream(f);
        //scp.ensureLocalWinAtLeast((int) Math.min(length, Integer.MAX_VALUE));
        signal("Remote can start transfer");
        transfer(scp.getInputStream(), fos, scp.getLocalMaxPacketSize(), length);
        check("Remote agrees transfer done");
        signal("Transfer done");
        IOUtils.closeQuietly(fos);
    }
    
    @Override
    protected void startCopy(String sourcePath, String targetPath) throws IOException
    {
        init(sourcePath);
        
        signal("Start status OK");
        
        String msg = readMessage(true);
        do
            process(null, msg, new File(targetPath));
        while ((msg = readMessage(false)) != null);
    }
    
    protected String[] tokenize(String msg, int numPartsExpected) throws IOException
    {
        String[] parts = msg.split(" ");
        if (parts.length != numPartsExpected)
            throw new IOException("Could not parse message received from remote SCP: " + msg);
        return parts;
    }
    
}
