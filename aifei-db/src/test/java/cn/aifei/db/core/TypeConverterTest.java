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

import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TypeConverterTest {

    private final TypeConverter converter = new TypeConverter();

    @Test
    public void byteAndShortValuesAreNormalizedToIntegerWithoutLoss() {
        assertEquals(Integer.valueOf(Byte.MIN_VALUE), converter.toInt(Byte.valueOf(Byte.MIN_VALUE)));
        assertEquals(Integer.valueOf(Byte.MAX_VALUE), converter.toInt(Byte.valueOf(Byte.MAX_VALUE)));
        assertEquals(Integer.valueOf(Short.MIN_VALUE), converter.toInt(Short.valueOf(Short.MIN_VALUE)));
        assertEquals(Integer.valueOf(Short.MAX_VALUE), converter.toInt(Short.valueOf(Short.MAX_VALUE)));
    }

    @Test
    public void booleanConversionAcceptsOnlyDocumentedNumberAndStringValues() {
        assertSame(Boolean.TRUE, converter.toBoolean(1));
        assertSame(Boolean.FALSE, converter.toBoolean(new BigDecimal("0.9")));
        assertSame(Boolean.TRUE, converter.toBoolean("true"));
        assertSame(Boolean.FALSE, converter.toBoolean("false"));
        assertSame(Boolean.TRUE, converter.toBoolean("1"));
        assertSame(Boolean.FALSE, converter.toBoolean("0"));
        assertNull(converter.toBoolean(null));

        assertIllegalArgument("Only 0 and 1", () -> converter.toBoolean(2));
        assertIllegalArgument("Only 'true', 'false', '1', and '0'", () -> converter.toBoolean("TRUE"));
        assertIllegalArgument(Object.class.getName(), () -> converter.toBoolean(new Object()));
    }

    @Test
    public void toDatePreservesJdbcAndJavaTimeSemantics() {
        Timestamp existing = Timestamp.valueOf("2026-07-13 12:34:56.123456789");
        assertSame(existing, converter.toDate(existing));
        assertNull(converter.toDate(null));

        LocalDateTime localDateTime = LocalDateTime.of(2026, 7, 13, 12, 34, 56, 123456789);
        java.util.Date convertedLocalDateTime = converter.toDate(localDateTime);
        assertTrue(convertedLocalDateTime instanceof Timestamp);
        assertEquals(localDateTime, ((Timestamp) convertedLocalDateTime).toLocalDateTime());

        LocalDate localDate = LocalDate.of(2026, 7, 13);
        java.util.Date convertedLocalDate = converter.toDate(localDate);
        assertTrue(convertedLocalDate instanceof java.sql.Date);
        assertFalse(convertedLocalDate instanceof Timestamp);
        assertEquals(localDate, ((java.sql.Date) convertedLocalDate).toLocalDate());
    }

    @Test
    public void instantBasedTemporalValuesPreserveTheirInstantAndNanoseconds() {
        Instant instant = Instant.parse("2026-07-13T10:34:56.123456789Z");
        OffsetDateTime offset = OffsetDateTime.ofInstant(instant, ZoneOffset.ofHours(2));
        ZonedDateTime zoned = ZonedDateTime.ofInstant(instant, ZoneOffset.ofHours(-5));

        assertTimestampInstant(instant, converter.toDate(instant));
        assertTimestampInstant(instant, converter.toDate(offset));
        assertTimestampInstant(instant, converter.toDate(zoned));
    }

    @Test
    public void dateConversionRejectsTimeOnlyAndUnsupportedValues() {
        assertIllegalArgument("without a date", () -> converter.toDate(LocalTime.NOON));
        assertIllegalArgument("without a date", () -> converter.toDate(
                OffsetTime.of(LocalTime.NOON, ZoneOffset.UTC)));
        assertIllegalArgument(Object.class.getName(), () -> converter.toDate(new Object()));
    }

    @Test
    public void dateStringsUseTimeUtilAutomaticPatternDetection() {
        java.util.Date value = converter.toDate("2026-7-13 12:34:56.789");

        assertEquals("2026-07-13 12:34:56.789", new Timestamp(value.getTime()).toString());
        assertIllegalArgument("Invalid date string", () -> converter.toDate("2026-02-30"));
    }

    @Test
    public void localDateTimeConversionPreservesJdbcTemporalMeaning() {
        Timestamp timestamp = Timestamp.valueOf("2026-07-13 12:34:56.123456789");
        assertEquals(timestamp.toLocalDateTime(), converter.toLocalDateTime(timestamp));
        assertEquals(LocalDateTime.of(2026, 7, 13, 0, 0),
                converter.toLocalDateTime(java.sql.Date.valueOf("2026-07-13")));
        assertEquals(LocalDateTime.of(2026, 7, 13, 0, 0),
                converter.toLocalDateTime(LocalDate.of(2026, 7, 13)));
        assertEquals(LocalDateTime.of(2026, 7, 13, 12, 34, 0),
                converter.toLocalDateTime("2026-7-13 12:34"));
        assertNull(converter.toLocalDateTime(null));

        assertIllegalArgument("without a date", () -> converter.toLocalDateTime(java.sql.Time.valueOf("12:34:56")));
        assertIllegalArgument("without a date", () -> converter.toLocalDateTime(LocalTime.NOON));
    }

    @Test
    public void localDateConversionUsesOnlyValuesThatContainARealDate() {
        Timestamp timestamp = Timestamp.valueOf("2026-07-13 23:59:59.999999999");
        LocalDate expected = LocalDate.of(2026, 7, 13);

        assertEquals(expected, converter.toLocalDate(java.sql.Date.valueOf("2026-07-13")));
        assertEquals(expected, converter.toLocalDate(timestamp));
        assertEquals(expected, converter.toLocalDate(timestamp.toLocalDateTime()));
        assertEquals(expected, converter.toLocalDate("2026-7-13 23:59:59"));
        assertSame(expected, converter.toLocalDate(expected));
        assertNull(converter.toLocalDate(null));

        assertIllegalArgument("without a date", () -> converter.toLocalDate(java.sql.Time.valueOf("12:34:56")));
        assertIllegalArgument("without a date", () -> converter.toLocalDate(LocalTime.NOON));
    }

    @Test
    public void timestampConversionPreservesLocalFieldsAndNanoseconds() {
        Timestamp existing = Timestamp.valueOf("2026-07-13 12:34:56.123456789");
        assertSame(existing, converter.toTimestamp(existing));

        LocalDateTime localDateTime = LocalDateTime.of(2026, 7, 13, 12, 34, 56, 987654321);
        assertEquals(localDateTime, converter.toTimestamp(localDateTime).toLocalDateTime());
        assertEquals(LocalDateTime.of(2026, 7, 13, 0, 0),
                converter.toTimestamp(LocalDate.of(2026, 7, 13)).toLocalDateTime());

        java.util.Date date = new java.util.Date(123456789L);
        assertEquals(date.getTime(), converter.toTimestamp(date).getTime());
        assertEquals(987654321L, converter.toTimestamp(987654321L).getTime());
        assertNull(converter.toTimestamp(null));

        assertIllegalArgument("without a date", () -> converter.toTimestamp(java.sql.Time.valueOf("12:34:56")));
        assertIllegalArgument(String.class.getName(), () -> converter.toTimestamp("2026-07-13"));
    }

    private static void assertTimestampInstant(Instant expected, java.util.Date actual) {
        assertTrue(actual instanceof Timestamp);
        assertEquals(expected, ((Timestamp) actual).toInstant());
    }

    private static void assertIllegalArgument(String messagePart, Runnable action) {
        try {
            action.run();
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException actual) {
            assertTrue("Unexpected message: " + actual.getMessage(), actual.getMessage().contains(messagePart));
        }
    }
}
