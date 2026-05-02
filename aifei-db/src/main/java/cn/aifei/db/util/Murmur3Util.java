/**
 *
 * murmurhash - Pure Java implementation of the Murmur Hash algorithms.
 * Copyright (c) 2014, Sandeep Gupta
 *
 * http://sangupta.com/projects/murmur
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package cn.aifei.db.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A pure Java implementation of the Murmur 3 hashing algorithm as presented
 * at <a href="https://sites.google.com/site/murmurhash/">Murmur Project</a>
 *
 * Source code :
 *   https://github.com/sangupta/murmur/blob/master/src/main/java/com/sangupta/murmur/Murmur3.java
 *
 * Code is ported from original C++ source at
 * <a href="https://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp">
 * MurmurHash3.cpp</a>
 *
 * @author sangupta
 * @since 1.0
 */
public class Murmur3Util {

    /**
     * Helps convert a byte into its unsigned value
     */
    public static final int UNSIGNED_MASK = 0xff;

    private static final long X64_128_C1 = 0x87c37b91114253d5L;
    private static final long X64_128_C2 = 0x4cf5ad432745937fL;

    /**
     * Compute the Murmur3 hash (128-bit version) as described in the original source code.
     *
     * @param data
     *            the data that needs to be hashed
     *
     * @param length
     *            the length of the data that needs to be hashed
     *
     * @param seed
     *            the seed to use to compute the hash
     *
     * @return the computed hash value
     */
    public static long[] hash_x64_128(final byte[] data, final int length, final long seed) {
        long h1 = seed;
        long h2 = seed;

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        while(buffer.remaining() >= 16) {
            long k1 = buffer.getLong();
            long k2 = buffer.getLong();

            h1 ^= mixK1(k1);

            h1 = Long.rotateLeft(h1, 27);
            h1 += h2;
            h1 = h1 * 5 + 0x52dce729;

            h2 ^= mixK2(k2);

            h2 = Long.rotateLeft(h2, 31);
            h2 += h1;
            h2 = h2 * 5 + 0x38495ab5;
        }

        // ----------
        // tail

        // Advance offset to the unprocessed tail of the data.
        // offset += (nblocks << 4); // nblocks * 16;

        buffer.compact();
        buffer.flip();

        final int remaining = buffer.remaining();
        if(remaining > 0) {
            long k1 = 0;
            long k2 = 0;
            switch (buffer.remaining()) {
                case 15:
                    k2 ^= (long) (buffer.get(14) & UNSIGNED_MASK) << 48;

                case 14:
                    k2 ^= (long) (buffer.get(13) & UNSIGNED_MASK) << 40;

                case 13:
                    k2 ^= (long) (buffer.get(12) & UNSIGNED_MASK) << 32;

                case 12:
                    k2 ^= (long) (buffer.get(11) & UNSIGNED_MASK) << 24;

                case 11:
                    k2 ^= (long) (buffer.get(10) & UNSIGNED_MASK) << 16;

                case 10:
                    k2 ^= (long) (buffer.get(9) & UNSIGNED_MASK) << 8;

                case 9:
                    k2 ^= (long) (buffer.get(8) & UNSIGNED_MASK);

                case 8:
                    k1 ^= buffer.getLong();
                    break;

                case 7:
                    k1 ^= (long) (buffer.get(6) & UNSIGNED_MASK) << 48;

                case 6:
                    k1 ^= (long) (buffer.get(5) & UNSIGNED_MASK) << 40;

                case 5:
                    k1 ^= (long) (buffer.get(4) & UNSIGNED_MASK) << 32;

                case 4:
                    k1 ^= (long) (buffer.get(3) & UNSIGNED_MASK) << 24;

                case 3:
                    k1 ^= (long) (buffer.get(2) & UNSIGNED_MASK) << 16;

                case 2:
                    k1 ^= (long) (buffer.get(1) & UNSIGNED_MASK) << 8;

                case 1:
                    k1 ^= (long) (buffer.get(0) & UNSIGNED_MASK);
                    break;

                default:
                    throw new AssertionError("Code should not reach here!");
            }

            // mix
            h1 ^= mixK1(k1);
            h2 ^= mixK2(k2);
        }

        // ----------
        // finalization

        h1 ^= length;
        h2 ^= length;

        h1 += h2;
        h2 += h1;

        h1 = fmix64(h1);
        h2 = fmix64(h2);

        h1 += h2;
        h2 += h1;

        return (new long[] { h1, h2 });
    }

    private static long mixK1(long k1) {
        k1 *= X64_128_C1;
        k1 = Long.rotateLeft(k1, 31);
        k1 *= X64_128_C2;

        return k1;
    }

    private static long mixK2(long k2) {
        k2 *= X64_128_C2;
        k2 = Long.rotateLeft(k2,  33);
        k2 *= X64_128_C1;

        return k2;
    }

    /**
     * fmix function for 64 bits.
     *
     * @param k
     * @return
     */
    private static long fmix64(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;

        return k;
    }
}




