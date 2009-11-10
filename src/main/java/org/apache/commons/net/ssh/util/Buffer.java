/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.net.ssh.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

import org.apache.commons.net.ssh.SSHRuntimeException;
import org.apache.commons.net.ssh.util.Constants.KeyType;
import org.apache.commons.net.ssh.util.Constants.Message;

/**
 * Provides support for reading and writing SSH binary data types.
 * 
 * Has convenient mappings from Java to SSH primitives.
 */
public class Buffer
{
    
    public static class BufferException extends SSHRuntimeException
    {
        public BufferException(String message)
        {
            super(message);
        }
    }
    
    /**
     * The default size for a {@code Buffer} (256 bytes)
     */
    public static final int DEFAULT_SIZE = 256;
    
    private static int getNextPowerOf2(int i)
    {
        int j = 1;
        while (j < i)
            j <<= 1;
        return j;
    }
    
    private byte[] data;
    
    private int rpos;
    private int wpos;
    
    /**
     * @see {@link #DEFAULT_SIZE}
     */
    public Buffer()
    {
        this(DEFAULT_SIZE);
    }
    
    public Buffer(Buffer from)
    {
        data = new byte[(wpos = from.wpos - from.rpos)];
        System.arraycopy(from.data, from.rpos, data, 0, wpos);
    }
    
    /**
     * @param data
     *            byte-array to initialise with
     */
    public Buffer(byte[] data)
    {
        this(data, true);
    }
    
    /**
     * @param data
     *            byte-array to initialise with
     * @param read
     *            whether write position should be advanced
     * 
     */
    public Buffer(byte[] data, boolean read)
    {
        this.data = data;
        rpos = 0;
        wpos = read ? data.length : 0;
    }
    
    public Buffer(int size)
    {
        this(new byte[getNextPowerOf2(size)], false);
    }
    
    /**
     * Constructs new buffer for the specified SSH packet and reserves the needed space (5 bytes)
     * for the packet header.
     * 
     * @param cmd
     *            the SSH command
     */
    public Buffer(Message msg)
    {
        this();
        rpos = wpos = 5;
        data[wpos++] = msg.toByte();
    }
    
    public byte[] array()
    {
        return data;
    }
    
    public int available()
    {
        return wpos - rpos;
    }
    
    /**
     * Resets this buffer. The object becomes ready for reuse.
     */
    public void clear()
    {
        rpos = 0;
        wpos = 0;
    }
    
    /**
     * Compact this {@link Buffer}
     */
    public void compact()
    {
        System.err.println("COMPACTING");
        if (available() > 0)
            System.arraycopy(data, rpos, data, 0, wpos - rpos);
        wpos -= rpos;
        rpos = 0;
    }
    
    /**
     * Read an SSH boolean byte
     * 
     * @return the {@code true} or {@code false} value read
     */
    public boolean readBoolean()
    {
        return readByte() != 0;
    }
    
    /**
     * Read a byte from the buffer
     * 
     * @return the byte read
     */
    public byte readByte()
    {
        ensureAvailable(1);
        return data[rpos++];
    }
    
    /**
     * Read an SSH byte-array
     * 
     * @return the byte-array read
     */
    public byte[] readBytes()
    {
        int len = readInt();
        if (len < 0 || len > 32768)
            throw new BufferException("Bad item length: " + len);
        byte[] b = new byte[len];
        readRawBytes(b);
        return b;
    }
    
    public byte[] getCompactData()
    {
        int len = available();
        if (len > 0)
        {
            byte[] b = new byte[len];
            System.arraycopy(data, rpos, b, 0, len);
            return b;
        } else
            return new byte[0];
    }
    
    public int readInt()
    {
        return (int) readLong();
    }
    
    public long readLong()
    {
        ensureAvailable(4);
        long i = data[rpos++] << 24 & 0xff000000L // 
                | data[rpos++] << 16 & 0x00ff0000L //
                | data[rpos++] << 8 & 0x0000ff00L //
                | data[rpos++] & 0x000000ffL;
        return i;
    }
    
    /**
     * Reads an SSH byte and returns it as {@link Constants.Message}
     * 
     * @return the message identifier
     */
    public Message readMessageID()
    {
        byte b = readByte();
        Message cmd = Message.fromByte(b);
        if (cmd == null)
            throw new BufferException("Unknown command code: " + b);
        return cmd;
    }
    
    /**
     * Read an SSH multiple-precision integer
     * 
     * @return the MP integer as a {@code BigInteger}
     */
    public BigInteger readMPInt()
    {
        return new BigInteger(readMPIntAsBytes());
    }
    
    /**
     * 
     * 
     * @return
     */
    public byte[] readMPIntAsBytes()
    {
        return readBytes();
    }
    
    public PublicKey readPublicKey()
    {
        PublicKey key = null;
        try
        {
            switch (KeyType.fromString(readString()))
            {
            case RSA:
            {
                BigInteger e = readMPInt();
                BigInteger n = readMPInt();
                KeyFactory keyFactory = SecurityUtils.getKeyFactory("RSA");
                key = keyFactory.generatePublic(new RSAPublicKeySpec(n, e));
                break;
            }
            case DSA:
            {
                BigInteger p = readMPInt();
                BigInteger q = readMPInt();
                BigInteger g = readMPInt();
                BigInteger y = readMPInt();
                KeyFactory keyFactory = SecurityUtils.getKeyFactory("DSA");
                key = keyFactory.generatePublic(new DSAPublicKeySpec(y, p, q, g));
                break;
            }
            default:
                assert false;
            }
        } catch (GeneralSecurityException e)
        {
            throw new SSHRuntimeException(e);
        }
        return key;
    }
    
    public Buffer readRawBytes(byte[] buf)
    {
        return readRawBytes(buf, 0, buf.length);
    }
    
    public Buffer readRawBytes(byte[] buf, int off, int len)
    {
        ensureAvailable(len);
        System.arraycopy(data, rpos, buf, off, len);
        rpos += len;
        return this;
    }
    
    /**
     * Reads an SSH string
     * 
     * @return the string as a Java {@code String}
     */
    public String readString()
    {
        int len = readInt();
        if (len < 0 || len > 32768)
            throw new BufferException("Bad item length: " + len);
        ensureAvailable(len);
        String s = null;
        try
        {
            s = new String(data, rpos, len, "UTF-8");
        } catch (UnsupportedEncodingException e)
        {
        }
        rpos += len;
        return s;
    }
    
    /**
     * Reads an SSH string
     * 
     * @return the string as a byte-array
     */
    public byte[] readStringAsBytes()
    {
        return readBytes();
    }
    
    /**
     * Gives a readable snapshot of the buffer in hex. This is useful for debugging.
     * 
     * @return snapshot of the buffer as a hex string with each octet delimited by a space
     */
    public String printHex()
    {
        return BufferUtils.printHex(array(), rpos(), available());
    }
    
    /**
     * Puts an SSH boolean value
     * 
     * @param b
     *            the value
     * @return this
     */
    public Buffer putBoolean(boolean b)
    {
        return putByte(b ? (byte) 1 : (byte) 0);
    }
    
    /**
     * Copies the contents of provided buffer into this buffer
     * 
     * @param buffer
     *            the {@code Buffer} to copy
     * @return this
     */
    public Buffer putBuffer(Buffer buffer)
    {
        if (buffer != null)
        {
            int r = buffer.available();
            ensureCapacity(r);
            System.arraycopy(buffer.data, buffer.rpos, data, wpos, r);
            wpos += r;
        }
        return this;
    }
    
    /**
     * Writes a single byte into this buffer
     * 
     * @param b
     * @return this
     */
    public Buffer putByte(byte b)
    {
        ensureCapacity(1);
        data[wpos++] = b;
        return this;
    }
    
    /**
     * Writes Java byte-array as an SSH byte-array
     * 
     * @param b
     *            Java byte-array
     * @return this
     */
    public Buffer putBytes(byte[] b)
    {
        return putBytes(b, 0, b.length);
    }
    
    /**
     * Writes Java byte-array as an SSH byte-array
     * 
     * @param b
     *            Java byte-array
     * @param off
     *            offset
     * @param len
     *            length
     * @return this
     */
    public Buffer putBytes(byte[] b, int off, int len)
    {
        putInt(len - off);
        return putRawBytes(b, off, len);
    }
    
    /**
     * Writes a uint32 integer
     * 
     * @param uint32
     * @return this
     */
    public Buffer putInt(long uint32)
    {
        ensureCapacity(4);
        if (uint32 < 0 || uint32 > 0xffffffffL)
            throw new BufferException("Invalid value: " + uint32);
        data[wpos++] = (byte) (uint32 >> 24);
        data[wpos++] = (byte) (uint32 >> 16);
        data[wpos++] = (byte) (uint32 >> 8);
        data[wpos++] = (byte) uint32;
        return this;
    }
    
    public Buffer putUINT64(long uint64)
    {
        if (uint64 < 0)
            throw new BufferException("Invalid value: " + uint64);
        putInt((uint64 & 0xffffffff00000000L) >>> 31);
        putInt(uint64 & 0x00000000ffffffffL);
        return this;
    }
    
    public long getUINT64()
    {
        long uint64 = (readLong() << 32) | readLong();
        if (uint64 < 0)
            throw new BufferException("Cannot handle values > Long.MAX_VALUE");
        return uint64;
    }
    
    /**
     * Writes a byte indicating the SSH message identifier
     * 
     * @param msg
     *            the identifier as a {@link Constants.Message} type
     * @return this
     */
    public Buffer putMessageID(Message msg)
    {
        return putByte(msg.toByte());
    }
    
    /**
     * Writes an SSH multiple-precision integer from a {@code BigInteger}
     * 
     * @param bi
     *            {@code BigInteger} to write
     * @return this
     */
    public Buffer putMPInt(BigInteger bi)
    {
        return putMPInt(bi.toByteArray());
    }
    
    /**
     * Writes an SSH multiple-precision integer from a Java byte-array
     * 
     * @param foo
     *            byte-array
     * @return this
     */
    public Buffer putMPInt(byte[] foo)
    {
        int i = foo.length;
        if ((foo[0] & 0x80) != 0)
        {
            i++;
            putInt(i);
            putByte((byte) 0);
        } else
            putInt(i);
        return putRawBytes(foo);
    }
    
    /**
     * Writes a char-array as an SSH string and then blanks it out.
     * 
     * This is useful when a plaintext password needs to be sent. If {@code passwd} is {@code null},
     * an empty string is written.
     * 
     * @param passwd
     *            (null-ok) the password as a character array
     * @return this
     */
    public Buffer putPassword(char[] passwd)
    {
        if (passwd == null)
            return putString("");
        putInt(passwd.length);
        ensureCapacity(passwd.length);
        for (char c : passwd)
            data[wpos++] = (byte) c;
        Arrays.fill(passwd, ' ');
        return this;
    }
    
    public Buffer putPublicKey(PublicKey key)
    {
        KeyType type = KeyType.fromKey(key);
        switch (type)
        {
        case RSA:
            putString(type.toString()) // ssh-rsa
                    .putMPInt(((RSAPublicKey) key).getPublicExponent()) // e
                    .putMPInt(((RSAPublicKey) key).getModulus()); // n
            break;
        case DSA:
            putString(type.toString()) // ssh-dss
                    .putMPInt(((DSAPublicKey) key).getParams().getP()) // p
                    .putMPInt(((DSAPublicKey) key).getParams().getQ()) // q
                    .putMPInt(((DSAPublicKey) key).getParams().getG()) // g
                    .putMPInt(((DSAPublicKey) key).getY()); // y
            break;
        default:
            assert false;
        }
        return this;
    }
    
    public Buffer putRawBytes(byte[] d)
    {
        return putRawBytes(d, 0, d.length);
    }
    
    public Buffer putRawBytes(byte[] d, int off, int len)
    {
        ensureCapacity(len);
        System.arraycopy(d, off, data, wpos, len);
        wpos += len;
        return this;
    }
    
    public Buffer putSignature(String sigFormat, byte[] sigData)
    {
        return putString(new Buffer() // signature blob as string
                .putString(sigFormat) // sig format identifier
                .putBytes(sigData) // sig as byte array
                .getCompactData());
    }
    
    public Buffer putString(byte[] str)
    {
        return putBytes(str);
    }
    
    public Buffer putString(byte[] str, int offset, int len)
    {
        return putBytes(str, offset, len);
    }
    
    public Buffer putString(String string)
    {
        return putString(string.getBytes());
    }
    
    public int rpos()
    {
        return rpos;
    }
    
    public void rpos(int rpos)
    {
        this.rpos = rpos;
    }
    
    @Override
    public String toString()
    {
        return "Buffer [rpos=" + rpos + ", wpos=" + wpos + ", size=" + data.length + "]";
    }
    
    public int wpos()
    {
        return wpos;
    }
    
    public void wpos(int wpos)
    {
        ensureCapacity(wpos - this.wpos);
        this.wpos = wpos;
    }
    
    private void ensureAvailable(int a)
    {
        if (available() < a)
            throw new BufferException("Underflow");
    }
    
    public void ensureCapacity(int capacity)
    {
        if (data.length - wpos < capacity)
        {
            int cw = wpos + capacity;
            byte[] tmp = new byte[getNextPowerOf2(cw)];
            System.arraycopy(data, 0, tmp, 0, data.length);
            data = tmp;
        }
    }
    
}