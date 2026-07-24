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

    public Boolean toBoolean(Object b) {
        if (b instanceof Boolean) {
            return (Boolean) b;
        } else if (b == null) {
            return null;
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
        if (d instanceof java.util.Date) {  // 支持由数据库而来的高频类型 java.sql.Timestamp
            return (java.util.Date) d;
        } else if (d == null) {
            return null;
        }

        if (d instanceof Temporal) {
            if (d instanceof LocalDateTime) {
                // 按 JDBC 本地日期时间语义保留字段与纳秒，不先转换成 Instant。
                return Timestamp.valueOf((LocalDateTime) d);
            }
            if (d instanceof LocalDate) {
                // java.sql.Date 是 java.util.Date 的子类，同时保留 SQL DATE 语义。
                return java.sql.Date.valueOf((LocalDate) d);
            }
            if (d instanceof Instant) {
                return Timestamp.from((Instant) d);
            }
            if (d instanceof OffsetDateTime) {
                // OffsetDateTime 表示确定的时间点，不能先调用 toLocalDateTime() 丢弃 offset。
                return Timestamp.from(((OffsetDateTime) d).toInstant());
            }
            if (d instanceof ZonedDateTime) {
                return Timestamp.from(((ZonedDateTime) d).toInstant());
            }
            if (d instanceof LocalTime || d instanceof OffsetTime) {
                throw new IllegalArgumentException("Cannot convert " + d.getClass().getSimpleName() + " to java.util.Date without a date.");
            }
        }

        if (d instanceof String) {
            return TimeUtil.parse((String) d);
        }

        throw new IllegalArgumentException("Cannot convert type " + d.getClass().getName() + " to java.util.Date.");
    }

    public LocalDateTime toLocalDateTime(Object ldt) {
        if (ldt instanceof LocalDateTime) {
            return (LocalDateTime) ldt;
        } else if (ldt == null) {
            return null;
        }

        if (ldt instanceof Timestamp) {
            return ((Timestamp) ldt).toLocalDateTime();
        }
        if (ldt instanceof java.sql.Date) {
            return ((java.sql.Date) ldt).toLocalDate().atStartOfDay();
        }
        if (ldt instanceof java.sql.Time) {
            throw new IllegalArgumentException("Cannot convert java.sql.Time to LocalDateTime without a date.");
        }
        if (ldt instanceof java.util.Date) {
            return TimeUtil.toLocalDateTime((java.util.Date) ldt);
        }
        if (ldt instanceof LocalDate) {
            return ((LocalDate) ldt).atStartOfDay();
        }
        if (ldt instanceof LocalTime) {
            throw new IllegalArgumentException("Cannot convert LocalTime to LocalDateTime without a date.");
        }

        if (ldt instanceof String) {
            return TimeUtil.parseLocalDateTime((String) ldt);
        }

        throw new IllegalArgumentException("Cannot convert type " + ldt.getClass().getName() + " to LocalDateTime.");
    }

    public LocalDate toLocalDate(Object ld) {
        if (ld instanceof LocalDate) {
            return (LocalDate) ld;
        } else if (ld == null) {
            return null;
        }

        if (ld instanceof java.sql.Date) {
            return ((java.sql.Date) ld).toLocalDate();
        }
        if (ld instanceof Timestamp) {
            return ((Timestamp) ld).toLocalDateTime().toLocalDate();
        }
        if (ld instanceof java.sql.Time) {
            throw new IllegalArgumentException("Cannot convert java.sql.Time to LocalDate without a date.");
        }
        if (ld instanceof java.util.Date) {
            return TimeUtil.toLocalDate((java.util.Date) ld);
        }
        if (ld instanceof LocalDateTime) {
            return ((LocalDateTime) ld).toLocalDate();
        }
        if (ld instanceof LocalTime) {
            throw new IllegalArgumentException("Cannot convert LocalTime to LocalDate without a date.");
        }
        if (ld instanceof String) {
            return TimeUtil.parseLocalDateTime((String) ld).toLocalDate();
        }

        throw new IllegalArgumentException("Cannot convert type " + ld.getClass().getName() + " to LocalDate.");
    }

    public Timestamp toTimestamp(Object ts) {
        if (ts instanceof Timestamp) {
            return (Timestamp) ts;
        } else if (ts == null) {
            return null;
        }

        if (ts instanceof java.sql.Time) {
            throw new IllegalArgumentException("Cannot convert java.sql.Time to Timestamp without a date.");
        }
        if (ts instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) ts).getTime());
        }
        if (ts instanceof LocalDateTime) {
            return Timestamp.valueOf((LocalDateTime) ts);
        }
        if (ts instanceof LocalDate) {
            return Timestamp.valueOf(((LocalDate) ts).atStartOfDay());
        }
        if (ts instanceof Long) {
            return new Timestamp((Long) ts);
        }

        throw new IllegalArgumentException("Cannot convert type " + ts.getClass().getName() + " to Timestamp.");
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
}
