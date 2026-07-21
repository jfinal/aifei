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

package cn.aifei.argument;

import cn.aifei.core.Input;
import cn.aifei.core.Output;
import cn.aifei.util.StrUtil;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.IsoEra;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Date;

/**
 * BasicArguments 针对 Java 基本类型实现 Argument 抽象。用户只需实现 Input 接口即可支持所有
 * Java 基本类型以及绝大多数其它类型的 action 参数注入。
 */
public class BasicArguments {

    public static class InputArgument extends Argument<Input, Output, Input> implements NoMatch {
        public Input getValue(Input input, Output output) {
            return input;
        }
    }

    public static class OutputArgument extends Argument<Input, Output, Output> implements NoMatch {
        public Output getValue(Input input, Output output) {
            return output;  // Dispatcher 中未创建 Output 对象时将返回 null
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * StrArgument
     */
    public static class StrArgument extends Argument<Input, Output, String> {
        public String getValue(Input input, Output output) {
            String ret = pathPara ? input.getStr(index) : input.getStr(name);
            return ret != null ? ret : defaultValue;
        }
        protected String parseDefaultValue(String str) {
            return str;     // 不能 trim()
        }
    }

    /**
     * IntArgument
     */
    public static class IntArgument extends Argument<Input, Output, Integer> {
        boolean isPrimitive;
        public void init(Parameter parameter) {
            super.init(parameter);
            isPrimitive = parameter.getType().isPrimitive();
        }
        public Integer getValue(Input input, Output output) {
            Integer ret = pathPara ? input.getInt(index) : input.getInt(name);
            if (ret != null) {
                return ret;
            } else if (defaultValue != null) {
                return defaultValue;
            } else if (isPrimitive) {
                throw new NullPointerException("Primitive type can not be null.");
            } else {
                return null;
            }
        }
        protected Integer parseDefaultValue(String str) {
            return Integer.valueOf(str);
        }
    }

    public static class IntArrayArgument extends Argument<Input, Output, int[]> {
        public void init(Parameter parameter) {
            super.init(parameter);
            if (pathPara) {
                throw new IllegalArgumentException("IntArrayArgument 不支持 path parameter");
            }
        }
        public int[] getValue(Input input, Output output) {
            Integer[] ret = input.getArray(name, Integer.class);
            return ret != null ? unbox(ret) : defaultValue;
        }
        private int[] unbox(Integer[] intArray) {
            int[] ret = new int[intArray.length];
            for (int i = 0; i < intArray.length; i++) {
                ret[i] = intArray[i];
            }
            return ret;
        }
        protected int[] parseDefaultValue(String str) {
            if (StrUtil.notBlank(str)) {
                if ("[]".equals(str)) { // 支持长度为 0 的数组
                    return new int[0];
                }

                String[] strArray = str.split(",");
                int[] ret = new int[strArray.length];
                for (int i = 0; i < strArray.length; i++) {
                    ret[i] = Integer.parseInt(strArray[i].trim());
                }
                return ret;
            }
            return null;
        }
    }

    /**
     * LongArgument
     */
    public static class LongArgument extends Argument<Input, Output, Long> {
        boolean isPrimitive;
        public void init(Parameter parameter) {
            super.init(parameter);
            isPrimitive = parameter.getType().isPrimitive();
        }
        public Long getValue(Input input, Output output) {
            Long ret = pathPara ? input.getLong(index) : input.getLong(name);
            if (ret != null) {
                return ret;
            } else if (defaultValue != null) {
                return defaultValue;
            } else if (isPrimitive) {
                throw new NullPointerException("Primitive type can not be null.");
            } else {
                return null;
            }
        }
        protected Long parseDefaultValue(String str) {
            return Long.valueOf(str);
        }
    }

    /**
     * LongArrayArgument
     */
    public static class LongArrayArgument extends Argument<Input, Output, long[]> {
        public void init(Parameter parameter) {
            super.init(parameter);
            if (pathPara) {
                throw new IllegalArgumentException("LongArrayArgument 不支持 path parameter");
            }
        }
        public long[] getValue(Input input, Output output) {
            Long[] ret = input.getArray(name, Long.class);
            return ret != null ? unbox(ret) : defaultValue;
        }
        private long[] unbox(Long[] longArray) {
            long[] ret = new long[longArray.length];
            for (int i = 0; i < longArray.length; i++) {
                ret[i] = longArray[i];
            }
            return ret;
        }
        protected long[] parseDefaultValue(String str) {
            if (StrUtil.notBlank(str)) {
                if ("[]".equals(str)) { // 支持长度为 0 的数组
                    return new long[0];
                }

                String[] strArray = str.split(",");
                long[] ret = new long[strArray.length];
                for (int i = 0; i < strArray.length; i++) {
                    ret[i] = Long.parseLong(strArray[i].trim());
                }
                return ret;
            }
            return null;
        }
    }

    /**
     * DoubleArgument
     */
    public static class DoubleArgument extends Argument<Input, Output, Double> {
        boolean isPrimitive;
        public void init(Parameter parameter) {
            super.init(parameter);
            isPrimitive = parameter.getType().isPrimitive();
        }
        public Double getValue(Input input, Output output) {
            Double ret = pathPara ? input.getDouble(index) : input.getDouble(name);
            if (ret != null) {
                return ret;
            } else if (defaultValue != null) {
                return defaultValue;
            } else if (isPrimitive) {
                throw new NullPointerException("Primitive type can not be null.");
            } else {
                return null;
            }
        }
        protected Double parseDefaultValue(String str) {
            return Double.valueOf(str);
        }
    }

    /**
     * DoubleArrayArgument
     */
    public static class DoubleArrayArgument extends Argument<Input, Output, double[]> {
        public void init(Parameter parameter) {
            super.init(parameter);
            if (pathPara) {
                throw new IllegalArgumentException("DoubleArrayArgument 不支持 path parameter");
            }
        }
        public double[] getValue(Input input, Output output) {
            Double[] ret = input.getArray(name, Double.class);
            return ret != null ? unbox(ret) : defaultValue;
        }
        private double[] unbox(Double[] doubleArray) {
            double[] ret = new double[doubleArray.length];
            for (int i = 0; i < doubleArray.length; i++) {
                ret[i] = doubleArray[i];
            }
            return ret;
        }
        protected double[] parseDefaultValue(String str) {
            if (StrUtil.notBlank(str)) {
                if ("[]".equals(str)) { // 支持长度为 0 的数组
                    return new double[0];
                }

                String[] strArray = str.split(",");
                double[] ret = new double[strArray.length];
                for (int i = 0; i < strArray.length; i++) {
                    ret[i] = Double.parseDouble(strArray[i].trim());
                }
                return ret;
            }
            return null;
        }
    }

    /**
     * BooleanArgument
     */
    public static class BooleanArgument extends Argument<Input, Output, Boolean> {
        boolean isPrimitive;
        public void init(Parameter parameter) {
            super.init(parameter);
            isPrimitive = parameter.getType().isPrimitive();
        }
        public Boolean getValue(Input input, Output output) {
            Boolean ret = pathPara ? input.getBoolean(index) : input.getBoolean(name);
            if (ret != null) {
                return ret;
            } else if (defaultValue != null) {
                return defaultValue;
            } else if (isPrimitive) {
                throw new NullPointerException("Primitive type can not be null.");
            } else {
                return null;
            }
        }
        protected Boolean parseDefaultValue(String str) {
            return Boolean.valueOf(str);
        }
    }

    /**
     * BooleanArrayArgument
     */
    public static class BooleanArrayArgument extends Argument<Input, Output, boolean[]> {
        public void init(Parameter parameter) {
            super.init(parameter);
            if (pathPara) {
                throw new IllegalArgumentException("BooleanArrayArgument 不支持 path parameter");
            }
        }
        public boolean[] getValue(Input input, Output output) {
            Boolean[] ret = input.getArray(name, Boolean.class);
            return ret != null ? unbox(ret) : defaultValue;
        }
        private boolean[] unbox(Boolean[] booleanArray) {
            boolean[] ret = new boolean[booleanArray.length];
            for (int i = 0; i < booleanArray.length; i++) {
                ret[i] = booleanArray[i];
            }
            return ret;
        }
        protected boolean[] parseDefaultValue(String str) {
            if (StrUtil.notBlank(str)) {
                if ("[]".equals(str)) { // 支持长度为 0 的数组
                    return new boolean[0];
                }

                String[] strArray = str.split(",");
                boolean[] ret = new boolean[strArray.length];
                for (int i = 0; i < strArray.length; i++) {
                    ret[i] = Boolean.parseBoolean(strArray[i].trim());
                }
                return ret;
            }
            return null;
        }
    }

    /**
     * BigDecimalArgument
     */
    public static class BigDecimalArgument extends Argument<Input, Output, BigDecimal> {
        public BigDecimal getValue(Input input, Output output) {
            BigDecimal ret = pathPara ? input.getBigDecimal(index) : input.getBigDecimal(name);
            return ret != null ? ret : defaultValue;
        }
        protected BigDecimal parseDefaultValue(String str) {
            return new BigDecimal(str);
        }
    }

    // --------------------------------------------------------------------------------------------

    private enum DatePattern {
        DATE_ONLY("yyyy-MM-dd"),
        DATE_TIME_TO_HOUR("yyyy-MM-dd HH"),
        DATE_TIME_TO_MINUTE("yyyy-MM-dd HH:mm"),
        DATE_TIME_TO_SECOND("yyyy-MM-dd HH:mm:ss"),
        DATE_TIME_TO_MILLISECOND("yyyy-MM-dd HH:mm:ss.SSS");

        final String pattern;
        final DateTimeFormatter formatter;

        DatePattern(String pattern) {
            this.pattern = pattern;
            this.formatter = createDateTimeFormatter(pattern);
        }

        static DatePattern detect(String str) {
            int space = str.indexOf(' ');
            if (space == -1) {
                return DATE_ONLY;
            }
            int firstColon = str.indexOf(':', space + 1);
            if (firstColon == -1) {
                return DATE_TIME_TO_HOUR;
            }
            int secondColon = str.indexOf(':', firstColon + 1);
            if (secondColon == -1) {
                return DATE_TIME_TO_MINUTE;
            }
            int dot = str.indexOf('.', secondColon + 1);
            if (dot == -1) {
                return DATE_TIME_TO_SECOND;
            }
            if (str.length() - dot - 1 != 3) {
                throw new IllegalArgumentException("Millisecond precision must contain exactly 3 digits: \"" + str + "\"");
            }
            return DATE_TIME_TO_MILLISECOND;
        }
    }

    private static DateTimeFormatter createDateTimeFormatter(String pattern) {
        return new DateTimeFormatterBuilder()
                // 作用于后续追加的解析规则，允许数字字段使用较少位数
                .parseLenient()
                .appendPattern(pattern)
                // yyyy 对应 YEAR_OF_ERA，严格模式下需要默认补充公元纪元
                .parseDefaulting(ChronoField.ERA, IsoEra.CE.getValue())
                // 仅有日期时默认为当天零点，分、秒和纳秒由解析器自动补零
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .toFormatter()
                // 拒绝越界值和不存在的日期，如 "2020-2-30"
                .withResolverStyle(ResolverStyle.STRICT);
    }

    /**
     * DateArgument
     */
    public static class DateArgument extends Argument<Input, Output, Date> {
        public void init(Parameter parameter) {
            super.init(parameter);
            if (pathPara) {
                throw new IllegalArgumentException("DateArgument 不支持 path parameter");
            }
        }
        public Date getValue(Input input, Output output) {
            Date ret = input.getDate(name);
            return ret != null ? ret : defaultValue;
        }
        protected Date parseDefaultValue(String str) {
            DatePattern datePattern = DatePattern.detect(str);
            SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern.pattern);
            dateFormat.setLenient(false);   // 允许数字字段不补零，但拒绝越界值和不存在的日期
            ParsePosition position = new ParsePosition(0);
            Date ret = dateFormat.parse(str, position);
            if (ret == null || position.getIndex() != str.length()) {
                throw new IllegalArgumentException("Invalid date string \"" + str + "\" for pattern \"" + datePattern.pattern + "\".");
            }
            return ret;
        }
    }

    /**
     * LocalDateTimeArgument
     */
    public static class LocalDateTimeArgument extends Argument<Input, Output, LocalDateTime> {
        public void init(Parameter parameter) {
            super.init(parameter);
            if (pathPara) {
                throw new IllegalArgumentException("LocalDateTimeArgument 不支持 path parameter");
            }
        }
        public LocalDateTime getValue(Input input, Output output) {
            LocalDateTime ret = input.getLocalDateTime(name);
            return ret != null ? ret : defaultValue;
        }
        protected LocalDateTime parseDefaultValue(String str) {
            return LocalDateTime.parse(str, DatePattern.detect(str).formatter);
        }
    }

    /**
     * LocalDateArgument
     */
    public static class LocalDateArgument extends Argument<Input, Output, LocalDate> {
        public void init(Parameter parameter) {
            super.init(parameter);
            if (pathPara) {
                throw new IllegalArgumentException("LocalDateArgument 不支持 path parameter");
            }
        }
        public LocalDate getValue(Input input, Output output) {
            LocalDate ret = input.getLocalDate(name);
            return ret != null ? ret : defaultValue;
        }
        protected LocalDate parseDefaultValue(String str) {
            return LocalDate.parse(str, DatePattern.DATE_ONLY.formatter);
        }
    }

    /**
     * LocalTimeArgument
     */
    public static class LocalTimeArgument extends Argument<Input, Output, LocalTime> {
        public void init(Parameter parameter) {
            super.init(parameter);
            if (pathPara) {
                throw new IllegalArgumentException("LocalTimeArgument 不支持 path parameter");
            }
        }
        public LocalTime getValue(Input input, Output output) {
            LocalTime ret = input.getLocalTime(name);
            return ret != null ? ret : defaultValue;
        }
        protected LocalTime parseDefaultValue(String str) {
            return LocalTime.parse(str, createDateTimeFormatter("HH:mm:ss"));
        }
    }
}

