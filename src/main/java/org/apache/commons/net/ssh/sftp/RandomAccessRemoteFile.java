package org.apache.commons.net.ssh.sftp;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

public class RandomAccessRemoteFile extends RemoteFile implements DataInput, DataOutput
{
    
    private final byte[] singleByte = new byte[1];
    
    private long fp;
    
    public RandomAccessRemoteFile(SFTPEngine sftp, String path, String handle)
    {
        super(sftp, path, handle);
    }
    
    public long getFilePointer()
    {
        return fp;
    }
    
    public void seek(long fp)
    {
        this.fp = fp;
    }
    
    public int read() throws IOException
    {
        return read(singleByte, 0, 1) == -1 ? -1 : singleByte[0];
    }
    
    public int read(byte[] b) throws IOException
    {
        return read(b, 0, b.length);
    }
    
    public int read(byte[] b, int off, int len) throws IOException
    {
        int count = read(fp, b, off, len);
        fp += count;
        return count;
    }
    
    public boolean readBoolean() throws IOException
    {
        final int ch = read();
        if (ch < 0)
            throw new EOFException();
        return (ch != 0);
    }
    
    public byte readByte() throws IOException
    {
        int ch = this.read();
        if (ch < 0)
            throw new EOFException();
        return (byte) (ch);
    }
    
    public char readChar() throws IOException
    {
        int ch1 = this.read();
        int ch2 = this.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (char) ((ch1 << 8) + (ch2 << 0));
    }
    
    public double readDouble() throws IOException
    {
        return Double.longBitsToDouble(readLong());
    }
    
    public float readFloat() throws IOException
    {
        return Float.intBitsToFloat(readInt());
    }
    
    public void readFully(byte[] b) throws IOException
    {
        readFully(b, 0, b.length);
    }
    
    public void readFully(byte[] b, int off, int len) throws IOException
    {
        int n = 0;
        do
        {
            int count = read(b, off + n, len - n);
            if (count < 0)
                throw new EOFException();
            n += count;
        } while (n < len);
    }
    
    public int readInt() throws IOException
    {
        final int ch1 = read();
        final int ch2 = read();
        final int ch3 = read();
        final int ch4 = read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }
    
    public String readLine() throws IOException
    {
        StringBuffer input = new StringBuffer();
        int c = -1;
        boolean eol = false;
        
        while (!eol)
            switch (c = read())
            {
            case -1:
            case '\n':
                eol = true;
                break;
            case '\r':
                eol = true;
                long cur = getFilePointer();
                if ((read()) != '\n')
                    seek(cur);
                break;
            default:
                input.append((char) c);
                break;
            }
        
        if ((c == -1) && (input.length() == 0))
            return null;
        return input.toString();
    }
    
    public long readLong() throws IOException
    {
        return ((long) (readInt()) << 32) + (readInt() & 0xFFFFFFFFL);
    }
    
    public short readShort() throws IOException
    {
        int ch1 = this.read();
        int ch2 = this.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (short) ((ch1 << 8) + (ch2 << 0));
    }
    
    public String readUTF() throws IOException
    {
        return DataInputStream.readUTF(this);
    }
    
    public int readUnsignedByte() throws IOException
    {
        int ch = this.read();
        if (ch < 0)
            throw new EOFException();
        return ch;
    }
    
    public int readUnsignedShort() throws IOException
    {
        int ch1 = this.read();
        int ch2 = this.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (ch1 << 8) + (ch2 << 0);
    }
    
    public int skipBytes(int n) throws IOException
    {
        if (n <= 0)
            return 0;
        final long pos = getFilePointer();
        final long len = length();
        long newpos = pos + n;
        if (newpos > len)
            newpos = len;
        seek(newpos);
        
        /* return the actual number of bytes skipped */
        return (int) (newpos - pos);
    }
    
    public void write(int i) throws IOException
    {
        singleByte[0] = (byte) i;
        write(singleByte);
    }
    
    public void write(byte[] b) throws IOException
    {
        write(b, 0, b.length);
    }
    
    public void write(byte[] b, int off, int len) throws IOException
    {
        write(fp, b, off, len);
        fp += (len - off);
    }
    
    public void writeBoolean(boolean v) throws IOException
    {
        write(v ? 1 : 0);
    }
    
    public void writeByte(int v) throws IOException
    {
        write(v);
    }
    
    public void writeBytes(String s) throws IOException
    {
        byte[] b = s.getBytes();
        write(b, 0, b.length);
    }
    
    public void writeChar(int v) throws IOException
    {
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
    }
    
    public void writeChars(String s) throws IOException
    {
        int clen = s.length();
        int blen = 2 * clen;
        byte[] b = new byte[blen];
        char[] c = new char[clen];
        s.getChars(0, clen, c, 0);
        for (int i = 0, j = 0; i < clen; i++)
        {
            b[j++] = (byte) (c[i] >>> 8);
            b[j++] = (byte) (c[i] >>> 0);
        }
        write(b, 0, blen);
    }
    
    public void writeDouble(double v) throws IOException
    {
        writeLong(Double.doubleToLongBits(v));
    }
    
    public void writeFloat(float v) throws IOException
    {
        writeInt(Float.floatToIntBits(v));
    }
    
    public void writeInt(int v) throws IOException
    {
        write((v >>> 24) & 0xFF);
        write((v >>> 16) & 0xFF);
        write((v >>> 8) & 0xFF);
        write(v & 0xFF);
    }
    
    public void writeLong(long v) throws IOException
    {
        write((int) (v >>> 56) & 0xFF);
        write((int) (v >>> 48) & 0xFF);
        write((int) (v >>> 40) & 0xFF);
        write((int) (v >>> 32) & 0xFF);
        write((int) (v >>> 24) & 0xFF);
        write((int) (v >>> 16) & 0xFF);
        write((int) (v >>> 8) & 0xFF);
        write((int) (v >>> 0) & 0xFF);
    }
    
    public void writeShort(int v) throws IOException
    {
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
    }
    
    public void writeUTF(String str) throws IOException
    {
        DataOutputStream dos = new DataOutputStream(new RemoteFileOutputStream(this, fp));
        dos.writeUTF(str);
        fp += dos.size();
    }
    
}
