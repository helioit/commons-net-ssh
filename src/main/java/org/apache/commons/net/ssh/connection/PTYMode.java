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
package org.apache.commons.net.ssh.connection;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.net.ssh.util.Buffer;

public enum PTYMode
{
    
    VINTR(1),
    VQUIT(2),
    VERASE(3),
    VKILL(4),
    VEOF(5),
    VEOL(6),
    VEOL2(7),
    VSTART(8),
    VSTOP(9),
    VSUSP(10),
    VDSUSP(11),
    VREPRINT(12),
    VWERASE(13),
    VLNEXT(14),
    VFLUSH(15),
    VSWTCH(16),
    VSTATUS(17),
    VDISCARD(18),
    IGNPAR(30),
    PARMRK(31),
    INPCK(32),
    ISTRIP(33),
    INLCR(34),
    IGNCR(35),
    ICRNL(36),
    IUCLC(37),
    IXON(38),
    IXANY(39),
    IXOFF(40),
    IMAXBEL(41),
    ISIG(50),
    ICANON(51),
    XCASE(52),
    ECHO(53),
    ECHOE(54),
    ECHOK(55),
    ECHONL(56),
    NOFLSH(57),
    TOSTOP(58),
    IEXTEN(59),
    ECHOCTL(60),
    ECHOKE(61),
    PENDIN(62),
    OPOST(70),
    OLCUC(71),
    ONLCR(72),
    OCRNL(73),
    ONOCR(74),
    ONLRET(75),
    CS7(90),
    CS8(91),
    PARENB(92),
    PARODD(93),
    TTY_OP_ISPEED(128),
    TTY_OP_OSPEED(129);
    
    public static byte[] encode(Map<PTYMode, Buffer> modes)
    {
        Buffer buf = new Buffer();
        for (Entry<PTYMode, Buffer> entry : modes.entrySet()) {
            buf.putByte(entry.getKey().getOpcode());
            buf.putBuffer(entry.getValue());
        }
        buf.putByte((byte) 0);
        return buf.getCompactData();
    }
    
    private final byte opcode;
    
    private PTYMode(int opcode)
    {
        this.opcode = (byte) opcode;
    }
    
    public byte getOpcode()
    {
        return opcode;
    }
    
}