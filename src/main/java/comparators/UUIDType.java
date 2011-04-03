package comparators;

/*
 * 
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
 * 
 */

import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.MarshalException;

/**
 * Compares UUIDs using the following criteria:<br>
 * - if count of supplied bytes is less than 16, compare counts<br>
 * - compare UUID version fields<br>
 * - nil UUID is always lesser<br>
 * - compare timestamps if both are time-based<br>
 * - compare lexically, unsigned msb-to-lsb comparison<br>
 * 
 * @author edanuff
 * @see "com.fasterxml.uuid.UUIDComparator"
 * 
 */
public class UUIDType extends AbstractType
{

    /**
	 * 
	 */
    public static final UUIDType instance = new UUIDType();

    UUIDType()
    {
    }

    @Override
    public int compare(ByteBuffer b1, ByteBuffer b2)
    {

        // Compare for length

        if ((b1 == null) || (b1.remaining() < 16))
        {
            return ((b2 == null) || (b2.remaining() < 16)) ? 0 : -1;
        }
        if ((b2 == null) || (b2.remaining() < 16))
        {
            return 1;
        }

        int s1 = b1.arrayOffset() + b1.position();
        byte[] o1 = b1.array();

        int s2 = b2.arrayOffset() + b2.position();
        byte[] o2 = b2.array();

        if (o1.length == s1)
        {
            return o2.length == s2 ? 0 : -1;
        }
        if (o2.length == s2)
        {
            return 1;
        }

        // Compare versions

        int v1 = (o1[s1 + 6] >> 4) & 0x0f;
        int v2 = (o2[s2 + 6] >> 4) & 0x0f;

        if (v1 != v2)
        {
            return v1 - v2;
        }

        // Compare timestamps for version 1

        if ((v1 == 1) && (v2 == 1))
        {
            // if both time-based, compare as timestamps
            int c = compareTimestampBytes(s1, o1, s2, o2);
            if (c != 0)
            {
                return c;
            }
        }

        // Compare the two byte arrays starting from the first
        // byte in the sequence until an inequality is
        // found. This should provide equivalent results
        // to the comparison performed by the RFC 4122
        // Appendix A - Sample Implementation.
        // Note: java.util.UUID.compareTo is not a lexical
        // comparison

        for (int i = 0; i < 16; i++)
        {
            int c = ((o1[s1 + i]) & 0xFF) - ((o2[s2 + i]) & 0xFF);
            if (c != 0)
            {
                return c;
            }
        }

        return 0;
    }

    private static int compareTimestampBytes(int s1, byte[] o1, int s2,
            byte[] o2)
    {
        int d = (o1[s1 + 6] & 0xF) - (o2[s2 + 6] & 0xF);
        if (d != 0)
        {
            return d;
        }
        d = (o1[s1 + 7] & 0xFF) - (o2[s2 + 7] & 0xFF);
        if (d != 0)
        {
            return d;
        }
        d = (o1[s1 + 4] & 0xFF) - (o2[s2 + 4] & 0xFF);
        if (d != 0)
        {
            return d;
        }
        d = (o1[s1 + 5] & 0xFF) - (o2[s2 + 5] & 0xFF);
        if (d != 0)
        {
            return d;
        }
        d = (o1[s1 + 0] & 0xFF) - (o2[s2 + 0] & 0xFF);
        if (d != 0)
        {
            return d;
        }
        d = (o1[s1 + 1] & 0xFF) - (o2[s2 + 1] & 0xFF);
        if (d != 0)
        {
            return d;
        }
        d = (o1[s1 + 2] & 0xFF) - (o2[s2 + 2] & 0xFF);
        if (d != 0)
        {
            return d;
        }
        return (o1[s1 + 3] & 0xFF) - (o2[s2 + 3] & 0xFF);
    }

    private static UUID getUUID(ByteBuffer bytes)
    {

        bytes = bytes.slice();
        if (bytes.remaining() < 16)
        {
            return new UUID(0, 0);
        }
        UUID uuid = new UUID(bytes.getLong(), bytes.getLong());
        return uuid;
    }

    @Override
    public void validate(ByteBuffer bytes)
    {
        if ((bytes.remaining() != 0) && (bytes.remaining() != 16))
        {
            throw new MarshalException("UUIDs must be exactly 16 bytes");
        }
    }

    @Override
    public String getString(ByteBuffer bytes)
    {
        if (bytes.remaining() == 0)
        {
            return "";
        }
        if (bytes.remaining() != 16)
        {
            throw new MarshalException("UUIDs must be exactly 16 bytes");
        }
        UUID uuid = getUUID(bytes);
        return uuid.toString();
    }
}
