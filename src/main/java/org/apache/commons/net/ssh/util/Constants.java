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

import java.security.Key;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * This interface defines symbolic names for constants.
 * 
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 * @author <a href="mailto:shikhar@schmizz.net">Shikhar Bhushan</a>
 */
public interface Constants
{
    
    /** Disconnect error codes */
    enum DisconnectReason
    {
        
        UNKNOWN(0),
        HOST_NOT_ALLOWED_TO_CONNECT(1),
        PROTOCOL_ERROR(2),
        KEY_EXCHANGE_FAILED(3),
        HOST_AUTHENTICATION_FAILED(4),
        RESERVED(4),
        MAC_ERROR(5),
        COMPRESSION_ERROR(6),
        SERVICE_NOT_AVAILABLE(7),
        PROTOCOL_VERSION_NOT_SUPPORTED(8),
        HOST_KEY_NOT_VERIFIABLE(9),
        CONNECTION_LOST(10),
        BY_APPLICATION(11),
        TOO_MANY_CONNECTIONS(12),
        AUTH_CANCELLED_BY_USER(13),
        NO_MORE_AUTH_METHODS_AVAILABLE(14),
        ILLEGAL_USER_NAME(15);
        
        public static DisconnectReason fromInt(int code)
        {
            for (DisconnectReason dc : values())
                if (dc.code == code)
                    return dc;
            return UNKNOWN;
        }
        
        private final int code;
        
        private DisconnectReason(int code)
        {
            this.code = code;
        }
        
        public int toInt()
        {
            return code;
        }
        
    }
    
    enum KeyType
    {
        
        /**
         * SSH identifier for RSA keys
         */
        RSA,

        /**
         * SSH identifier for DSA keys
         */
        DSA,

        /**
         * Unrecognized
         */
        UNKNOWN;
        
        public static KeyType fromKey(Key key)
        {
            if (key instanceof RSAPublicKey || key instanceof RSAPrivateKey)
                return RSA;
            else if (key instanceof DSAPublicKey || key instanceof DSAPrivateKey)
                return DSA;
            return UNKNOWN;
        }
        
        public static KeyType fromString(String sType)
        {
            for (KeyType kt : values())
                if (kt.toString().equals(sType))
                    return kt;
            return UNKNOWN;
        }
        
        @Override
        public String toString()
        {
            switch (this)
            {
            case RSA:
                return "ssh-rsa";
            case DSA:
                return "ssh-dss";
            default:
                return "unknown";
            }
        }
        
    }
    
    /** SSH message identifiers */
    enum Message
    {
        
        DISCONNECT(1),
        IGNORE(2),
        UNIMPLEMENTED(3),
        DEBUG(4),
        SERVICE_REQUEST(5),
        SERVICE_ACCEPT(6),
        KEXINIT(20),
        NEWKEYS(21),
        
        KEXDH_INIT(30),
        
        /**
         * { KEXDH_REPLY, KEXDH_GEX_GROUP }
         */
        KEXDH_31(31),
        
        KEX_DH_GEX_INIT(32),
        KEX_DH_GEX_REPLY(33),
        KEX_DH_GEX_REQUEST(34),
        
        USERAUTH_REQUEST(50),
        USERAUTH_FAILURE(51),
        USERAUTH_SUCCESS(52),
        USERAUTH_BANNER(53),
        
        /**
         * { USERAUTH_PASSWD_CHANGREQ, USERAUTH_PK_OK, USERAUTH_INFO_REQUEST }
         */
        USERAUTH_60(60),
        USERAUTH_INFO_RESPONSE(61),
        
        GLOBAL_REQUEST(80),
        REQUEST_SUCCESS(81),
        REQUEST_FAILURE(82),
        
        CHANNEL_OPEN(90),
        CHANNEL_OPEN_CONFIRMATION(91),
        CHANNEL_OPEN_FAILURE(92),
        CHANNEL_WINDOW_ADJUST(93),
        CHANNEL_DATA(94),
        CHANNEL_EXTENDED_DATA(95),
        CHANNEL_EOF(96),
        CHANNEL_CLOSE(97),
        CHANNEL_REQUEST(98),
        CHANNEL_SUCCESS(99),
        CHANNEL_FAILURE(100);
        
        private final byte b;
        
        static Message[] commands;
        
        static {
            commands = new Message[256];
            for (Message c : Message.values())
                if (commands[c.toByte()] == null)
                    commands[c.toByte()] = c;
        }
        
        public static Message fromByte(byte b)
        {
            return commands[b];
        }
        
        private Message(int b)
        {
            this.b = (byte) b;
        }
        
        public boolean geq(int num)
        {
            return b >= num;
        }
        
        public boolean gt(int num)
        {
            return b > num;
        }
        
        public boolean in(int x, int y)
        {
            return b >= x && b <= y;
        }
        
        public boolean leq(int num)
        {
            return b <= num;
        }
        
        public boolean lt(int num)
        {
            return b < num;
        }
        
        public byte toByte()
        {
            return b;
        }
        
    }
    
}
