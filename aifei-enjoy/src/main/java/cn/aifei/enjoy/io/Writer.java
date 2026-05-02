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

import cn.aifei.enjoy.util.TimeUtil;

import java.io.IOException;
import java.util.Date;

/**
 * Writer
 */
public abstract class Writer implements AutoCloseable {

    protected DateFormats formats = new DateFormats();

    public abstract void flush() throws IOException;

    public abstract void write(IWritable writable) throws IOException;

    public abstract void write(String string, int offset, int length) throws IOException;

    public abstract void write(String string) throws IOException;

    public abstract void write(StringBuilder stringBuilder, int offset, int length) throws IOException;

    public abstract void write(StringBuilder stringBuilder) throws IOException;

    public abstract void write(boolean booleanValue) throws IOException;

    public abstract void write(int intValue) throws IOException;

    public abstract void write(long longValue) throws IOException;

    public abstract void write(double doubleValue) throws IOException;

    public abstract void write(float floatValue) throws IOException;

    public void write(short shortValue) throws IOException {
        write((int)shortValue);
    }

    public void write(Date date, String datePattern) throws IOException {
        String str = formats.getDateFormat(datePattern).format(date);
        write(str, 0, str.length());
    }

    /**
     * 格式化输出 LocalDateTime、LocalDate、LocalTime
     */
    public void write(java.time.temporal.Temporal temporal, String pattern) throws IOException {
        write(TimeUtil.getDateTimeFormatter(pattern).format(temporal));
    }
}







