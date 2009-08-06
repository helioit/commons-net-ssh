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
    
    public SCPDownloadClient(SSHClient host)
    {
        this(host, null);
    }
    
    public SCPDownloadClient(SSHClient host, ModeSetter modeSetter)
    {
        super(host);
        this.modeSetter = modeSetter == null ? new DefaultModeSetter() : modeSetter;
    }
    
    protected boolean doCopy(File f) throws IOException
    {
        String msg = readMessage();
        String bufferedTMsg = null;
        switch (msg.charAt(0))
        {
            case 'T':
                bufferedTMsg = msg;
                sendOK();
                break;
            case 'C':
                processIncomingFile(msg, bufferedTMsg, f);
                break;
            case 'D':
                processIncomingDirectory(msg, bufferedTMsg, f);
                break;
            case 'E':
                return true;
            default:
                // TODO: send error to remote scp, then raise IOEx
                assert false;
        }
        return false;
    }
    
    protected void init(String source) throws ConnectionException, TransportException
    {
        List<String> args = new LinkedList<String>();
        args.add(Arg.SOURCE.toString());
        args.add(Arg.QUIET.toString());
        args.add(Arg.RECURSIVE.toString());
        if (modeSetter.shouldPreserveTimes())
            args.add(Arg.PRESERVE_MODES.toString());
        args.add(source == null || source.equals("") ? "." : source);
        execSCPWith(args);
    }
    
    protected void mkdir(File f) throws IOException
    {
        if (!f.mkdir())
            throw new IOException("Failed to create directory " + f);
    }
    
    protected long parseLong(String longString, String valType) throws IOException
    {
        long val = 0;
        try {
            val = Long.parseLong(longString);
        } catch (NumberFormatException nfe) {
            throw new IOException("Could not parse " + valType + " from: " + longString, nfe);
        }
        return val;
    }
    
    protected String parsePermissions(String cmd) throws IOException
    {
        // e.g. "C0644" -> "644"; "D0755" -> "755"
        if (cmd.length() != 5)
            throw new IOException("Could not parse permissions: " + cmd);
        return cmd.substring(2);
    }
    
    protected void processIncomingDirectory(String dMsg, String tMsg, File f) throws IOException
    {
        String[] dMsgParts = tokenize(dMsg, 3);
        String perms = parsePermissions(dMsgParts[0]);
        long length = parseLong(dMsgParts[1], "dir length");
        if (length != 0)
            throw new IOException("Remote SCP command sent strange directory length: " + length);
        String dirname = dMsgParts[2];
        
        if (f.exists())
            if (f.isDirectory())
                f = new File(f, dirname);
            else
                throw new IOException(f + " already exists as a file; remote end was sending directory");
        
        if (!f.exists())
            mkdir(f);
        
        modeSetter.setPermissions(f, perms);
        setTimes(tMsg, f);
        
        sendOK();
        
        while (!doCopy(f))
            ;
        
        sendOK();
    }
    
    protected void processIncomingFile(String cMsg, String tMsg, File f) throws IOException
    {
        String[] cMsgParts = tokenize(cMsg, 3);
        String perms = parsePermissions(cMsgParts[0]);
        long length = parseLong(cMsgParts[1], "length");
        String filename = cMsgParts[2];
        
        if (f.exists()) {
            if (f.isDirectory())
                f = new File(f, filename);
        } else
            f.createNewFile();
        
        modeSetter.setPermissions(f, perms);
        setTimes(tMsg, f);
        
        scp.ensureLocalWinAtLeast((int) Math.min(length, Integer.MAX_VALUE));
        
        FileOutputStream fos = new FileOutputStream(f);
        sendOK();
        transfer(scp.getInputStream(), fos, scp.getLocalMaxPacketSize(), length);
        checkResponseOK();
        
        IOUtils.closeQuietly(fos);
        
        sendOK();
    }
    
    protected void setTimes(String tMsg, File f) throws IOException
    {
        if (tMsg != null && modeSetter.shouldPreserveTimes()) {
            String[] tMsgParts = tokenize(tMsg, 4);
            modeSetter.setLastModifiedTime(f, parseLong(tMsgParts[0].substring(1), "last modified time"));
            modeSetter.setLastAccessedTime(f, parseLong(tMsgParts[2], "last access time"));
        }
    }
    
    @Override
    protected void startCopy(String sourcePath, String targetPath) throws IOException
    {
        init(sourcePath);
        sendOK();
        doCopy(new File(targetPath));
    }
    
    protected String[] tokenize(String msg, int numPartsExpected) throws IOException
    {
        String[] parts = msg.split(" ");
        if (parts.length != numPartsExpected)
            throw new IOException("Could not parse message received from remote SCP: " + msg);
        return parts;
    }
    
}
