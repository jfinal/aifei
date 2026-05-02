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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

    private static String detectDatePattern(String str) {
        String pattern;
        if (       str.length() > "yyyy-MM-dd HH:mm:ss".length()) {
            pattern = "yyyy-MM-dd HH:mm:ss.SSS";
        } else if (str.length() > "yyyy-MM-dd HH:mm".length()) {
            pattern = "yyyy-MM-dd HH:mm:ss";
        } else if (str.length() > "yyyy-MM-dd HH".length()) {
            pattern = "yyyy-MM-dd HH:mm";
        } else if (str.length() > "yyyy-MM-dd".length()) {
            pattern = "yyyy-MM-dd HH";
        } else {
            pattern = "yyyy-MM-dd";
        }
        return pattern;
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
            try {
                String pattern = detectDatePattern(str);
                return new SimpleDateFormat(pattern).parse(str);
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
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
            return LocalDate.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
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
            return LocalTime.parse(str, DateTimeFormatter.ofPattern("HH:mm:ss"));
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
            String pattern = detectDatePattern(str);
            return LocalDateTime.parse(str, DateTimeFormatter.ofPattern(pattern));
        }
    }
}




