/*
 * Copyright 2011-2035 詹波 (aifei.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.aifei.enjoy.io;

import java.io.IOException;

/**
 * FloatingWriter
 */
public class FloatingWriter {

    public static void write(ByteWriter byteWriter, double doubleValue) throws IOException {
        FloatingDecimal fd = new FloatingDecimal(doubleValue);
        char[] chars = byteWriter.chars;
        byte[] bytes = byteWriter.bytes;
        int len = fd.getChars(chars);
        for (int i=0; i<len; i++) {
            bytes[i] = (byte)chars[i];
        }
        byteWriter.out.write(bytes, 0, len);
    }

    public static void write(ByteWriter byteWriter, float floatValue) throws IOException {
        FloatingDecimal fd = new FloatingDecimal(floatValue);
        char[] chars = byteWriter.chars;
        byte[] bytes = byteWriter.bytes;
        int len = fd.getChars(chars);
        for (int i=0; i<len; i++) {
            bytes[i] = (byte)chars[i];
        }
        byteWriter.out.write(bytes, 0, len);
    }

    public static void write(CharWriter charWriter, double doubleValue) throws IOException {
        FloatingDecimal fd = new FloatingDecimal(doubleValue);
        char[] chars = charWriter.chars;
        int len = fd.getChars(chars);
        charWriter.out.write(chars, 0, len);
    }

    public static void write(CharWriter charWriter, float floatValue) throws IOException {
        FloatingDecimal fd = new FloatingDecimal(floatValue);
        char[] chars = charWriter.chars;
        int len = fd.getChars(chars);
        charWriter.out.write(chars, 0, len);
    }
}






