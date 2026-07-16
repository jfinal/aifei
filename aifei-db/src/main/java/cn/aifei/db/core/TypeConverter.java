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

package cn.aifei.db.core;

import cn.aifei.enjoy.util.TimeUtil;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.Temporal;

/**
 * TypeConverter
 */
public class TypeConverter implements Serializable {

    private static final String datePattern = "yyyy-MM-dd";
    private static final int dateLen = datePattern.length();

    private static final String dateTimeWithoutSecondPattern = "yyyy-MM-dd HH:mm";
    private static final int dateTimeWithoutSecondLen = dateTimeWithoutSecondPattern.length();

    private static final String dateTimePattern = "yyyy-MM-dd HH:mm:ss";

    public String toStr(Object s) {
        if (s instanceof String) {
            return (String) s;
        } else if (s == null) {
            return null;
        } else {
            return s.toString();
        }
    }

    public Integer toInt(Object n) {
        if (n instanceof Integer) {
            return (Integer) n;
        } else if (n == null) {
            return null;
        } else if (n instanceof Number) {
            return ((Number) n).intValue();
        } else {
            return Integer.valueOf(n.toString());   // 支持 String 类型转换
        }
    }

    public Long toLong(Object n) {
        if (n instanceof Long) {
            return (Long) n;
        } else if (n == null) {
            return null;
        } else if (n instanceof Number) {
            return ((Number) n).longValue();
        } else {
            return Long.valueOf(n.toString());  // 支持 String 类型转换
        }
    }

    public Double toDouble(Object n) {
        if (n instanceof Double) {
            return (Double) n;
        } else if (n == null) {
            return null;
        } else if (n instanceof Number) {
            return ((Number) n).doubleValue();
        } else {
            return Double.valueOf(n.toString());    // 支持 String 类型转换
        }
    }

    public BigDecimal toBigDecimal(Object n) {
        if (n instanceof BigDecimal) {
            return (BigDecimal) n;
        } else if (n == null) {
            return null;
        } else {
            return new BigDecimal(n.toString());    // 支持 String 类型转换
        }
    }

    public BigInteger toBigInteger(Object n) {
        if (n instanceof BigInteger) {
            return (BigInteger) n;
        } else if (n == null) {
            return null;
        }

        // 数据类型 id(19 number)在 Oracle Jdbc 下对应的是 BigDecimal,
        // 但是在 MySql 下对应的是 BigInteger，这会导致在 MySql 下生成的代码无法在 Oracle 数据库中使用
        if (n instanceof BigDecimal) {
            return ((BigDecimal) n).toBigInteger();
        } else if (n instanceof Number) {
            return BigInteger.valueOf(((Number) n).longValue());
        } else if (n instanceof String) {
            return new BigInteger((String) n);
        }

        throw new IllegalArgumentException("Cannot convert type " + n.getClass().getName() + " to BigInteger.");
    }

    public Float toFloat(Object n) {
        if (n instanceof Float) {
            return (Float) n;
        } else if (n == null) {
            return null;
        } else if (n instanceof Number) {
            return ((Number) n).floatValue();
        } else {
            return Float.valueOf(n.toString()); // 支持 String 类型转换
        }
    }

    // public Short toShort(Object n) {
    //     if (n instanceof Short) {
    //         return (Short) n;
    //     } else if (n == null) {
    //         return null;
    //     } else if (n instanceof Number) {
    //         return ((Number) n).shortValue();
    //     } else {
    //         return Short.valueOf(n.toString()); // 支持 String 类型转换
    //     }
    // }

    // public Byte toByte(Object n) {
    //     if (n instanceof Byte) {
    //         return (Byte) n;
    //     } else if (n == null) {
    //         return null;
    //     } else if (n instanceof Number) {
    //         return ((Number) n).byteValue();
    //     } else {
    //         return Byte.valueOf(n.toString());  // 支持 String 类型转换
    //     }
    // }

    public Boolean toBoolean(Object b) {
        if (b instanceof Boolean) {
            return (Boolean) b;
        } else if (b == null) {
            return null;
        }

        // 支持 String
        if (b instanceof String) {
            String s = (String) b;
            if ("true".equals(s) || "1".equals(s)) {
                return Boolean.TRUE;
            } else if ("false".equals(s) || "0".equals(s)) {
                return Boolean.FALSE;
            } else {
                throw new IllegalArgumentException("Cannot convert String value \"" + s + "\" to Boolean. Only 'true', 'false', '1', and '0' are supported.");
            }
        }

        // 支持 Number
        if (b instanceof Number) {
            int n = ((Number) b).intValue();
            if (n == 1) {
                return Boolean.TRUE;
            } else if (n == 0) {
                return Boolean.FALSE;
            } else {
                throw new IllegalArgumentException("Cannot convert Number value " + n + " to Boolean. Only 0 and 1 are supported.");
            }
        }

        throw new IllegalArgumentException("Cannot convert type " + b.getClass().getName() + " to Boolean.");
    }

    public Number toNumber(Object n) {
        if (n instanceof Number) {
            return (Number) n;
        } else if (n == null) {
            return null;
        }

        // 支持 String 类型转换
        // String s = n.toString();
        // return s.indexOf('.') != -1 ? Double.parseDouble(s) : Long.parseLong(s);
        return new BigDecimal(n.toString());
    }

    public java.util.Date toDate(Object d) {
        if (d instanceof java.util.Date) {
            return (java.util.Date) d;
        } else if (d == null) {
            return null;
        }

        if (d instanceof Temporal) {
            if (d instanceof LocalDateTime) {
                return TimeUtil.toDate((LocalDateTime) d);
            }
            if (d instanceof LocalDate) {
                return TimeUtil.toDate((LocalDate) d);
            }
            if (d instanceof LocalTime) {
                return TimeUtil.toDate((LocalTime) d);
            }
            if (d instanceof OffsetDateTime) {
                // OffsetDateTime 表示确定的时间点，不能先调用 toLocalDateTime() 丢弃 offset。
                return java.util.Date.from(((OffsetDateTime) d).toInstant());
            }
            if (d instanceof ZonedDateTime) {
                return java.util.Date.from(((ZonedDateTime) d).toInstant());
            }
        }

        if (d instanceof String) {
            String s = (String) d;
            if (s.length() <= dateLen) {
                return TimeUtil.parse(s, datePattern);
            } else if (s.length() > dateTimeWithoutSecondLen) {
                return TimeUtil.parse(s, dateTimePattern);
            } else {
                // 判断冒号字符是否出现两次，月、日、小时、分、秒都允许是一位数，例如：2022-1-2 3:4:5
                int index = s.indexOf(':');
                if (index != -1) {
                    if (index != s.lastIndexOf(':')) {
                        return TimeUtil.parse(s, dateTimePattern);
                    } else {
                        return TimeUtil.parse(s, dateTimeWithoutSecondPattern);
                    }
                }
            }
            throw new IllegalArgumentException("Unrecognized Date string format: \"" + s + "\"");
        }

        throw new IllegalArgumentException("Cannot convert type " + d.getClass().getName() + " to java.util.Date.");
    }

    public LocalDateTime toLocalDateTime(Object ldt) {
        if (ldt instanceof LocalDateTime) {
            return (LocalDateTime) ldt;
        } else if (ldt == null) {
            return null;
        }

        if (ldt instanceof java.util.Date) {
            return TimeUtil.toLocalDateTime((java.util.Date) ldt);
        }
        if (ldt instanceof LocalDate) {
            return ((LocalDate) ldt).atStartOfDay();
        }
        if (ldt instanceof LocalTime) {
            // LocalTime 无法单独转为 LocalDateTime，这里默认使用当前日期填充
            return LocalDateTime.of(LocalDate.now(), (LocalTime) ldt);
        }

        if (ldt instanceof String) {
            String s = (String) ldt;
            if (s.length() <= dateLen) {
                return TimeUtil.parseLocalDateTime(s, datePattern);
            } else if (s.length() > dateTimeWithoutSecondLen) {
                return TimeUtil.parseLocalDateTime(s, dateTimePattern);
            } else {
                // 判断冒号字符是否出现两次，月、日、小时、分、秒都允许是一位数，例如：2022-1-2 3:4:5
                int index = s.indexOf(':');
                if (index != -1) {
                    if (index != s.lastIndexOf(':')) {
                        return TimeUtil.parseLocalDateTime(s, dateTimePattern);
                    } else {
                        return TimeUtil.parseLocalDateTime(s, dateTimeWithoutSecondPattern);
                    }
                }
            }
            throw new IllegalArgumentException("Unrecognized LocalDateTime string format: \"" + s + "\"");
        }

        throw new IllegalArgumentException("Cannot convert type " + ldt.getClass().getName() + " to LocalDateTime.");
    }

    public LocalDate toLocalDate(Object ld) {
        if (ld instanceof LocalDate) {
            return (LocalDate) ld;
        } else if (ld == null) {
            return null;
        } else if (ld instanceof java.util.Date) {
            return TimeUtil.toLocalDate((java.util.Date) ld);
        } else {
            return toLocalDateTime(ld).toLocalDate();
        }
    }

    public Timestamp toTimestamp(Object ts) {
        if (ts instanceof Timestamp) {
            return (Timestamp) ts;
        } else if (ts == null) {
            return null;
        } else if (ts instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) ts).getTime());
        } else if (ts instanceof LocalDateTime) {
            return Timestamp.from(((LocalDateTime) ts).atZone(ZoneId.systemDefault()).toInstant());
        } else if (ts instanceof LocalDate) {
            return Timestamp.from(((LocalDate) ts).atStartOfDay(ZoneId.systemDefault()).toInstant());
        } else if (ts instanceof Long) {
            return new Timestamp((Long) ts);
        }

        throw new IllegalArgumentException("Cannot convert type " + ts.getClass().getName() + " to Timestamp.");
    }
}
