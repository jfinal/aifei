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

package cn.aifei.db.generator;

import org.junit.Test;

import java.net.URL;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TypeMappingTest {

    private final TypeMapping mapping = new TypeMapping();

    @Test
    public void byteAndShortAreNormalizedToIntegerForClassAndJdbcMappings() {
        assertEquals(Integer.class.getName(), mapping.getType(Byte.class.getName()));
        assertEquals(Integer.class.getName(), mapping.getType(Short.class.getName()));
        assertEquals(Integer.class.getName(), mapping.getType(Types.TINYINT));
        assertEquals(Integer.class.getName(), mapping.getType(Types.SMALLINT));
        assertEquals(Integer.class.getName(), mapping.getType(Types.INTEGER));
    }

    @Test
    public void temporalMappingsPreserveValueSemanticsAtGeneratedApiBoundary() {
        assertEquals(java.util.Date.class.getName(), mapping.getType(java.util.Date.class.getName()));
        assertEquals(java.util.Date.class.getName(), mapping.getType(java.sql.Timestamp.class.getName()));
        assertEquals(java.util.Date.class.getName(), mapping.getType(LocalDateTime.class.getName()));

        assertEquals(LocalDate.class.getName(), mapping.getType(java.sql.Date.class.getName()));
        assertEquals(LocalTime.class.getName(), mapping.getType(java.sql.Time.class.getName()));
        assertEquals(LocalDate.class.getName(), mapping.getType(LocalDate.class.getName()));
        assertEquals(LocalTime.class.getName(), mapping.getType(LocalTime.class.getName()));

        assertEquals(java.util.Date.class.getName(), mapping.getType(Types.TIMESTAMP));
        assertEquals(LocalDate.class.getName(), mapping.getType(Types.DATE));
        assertEquals(LocalTime.class.getName(), mapping.getType(Types.TIME));
    }

    @Test
    public void timezoneMappingsRequireAnExactOffsetRuntimeClass() {
        assertEquals(OffsetDateTime.class.getName(), mapping.getType(OffsetDateTime.class.getName()));
        assertEquals(OffsetTime.class.getName(), mapping.getType(OffsetTime.class.getName()));

        assertNull(mapping.getType(Types.TIMESTAMP_WITH_TIMEZONE));
        assertNull(mapping.getType(Types.TIME_WITH_TIMEZONE));
        assertNull(mapping.getType("vendor.TimestampWithTimeZone"));
    }

    @Test
    public void jdbcFallbackCoversNumericTextLobAndStandardObjectFamilies() {
        assertEquals(Long.class.getName(), mapping.getType(Types.BIGINT));
        assertEquals(java.math.BigDecimal.class.getName(), mapping.getType(Types.NUMERIC));
        assertEquals(Boolean.class.getName(), mapping.getType(Types.BOOLEAN));

        assertEquals("byte[]", mapping.getType(Types.LONGVARBINARY));
        assertEquals("byte[]", mapping.getType(Types.BLOB));
        assertEquals(String.class.getName(), mapping.getType(Types.NVARCHAR));
        assertEquals(String.class.getName(), mapping.getType(Types.NCLOB));

        assertEquals(java.sql.Array.class.getName(), mapping.getType(Types.ARRAY));
        assertEquals(java.sql.Struct.class.getName(), mapping.getType(Types.STRUCT));
        assertEquals(java.sql.Ref.class.getName(), mapping.getType(Types.REF));
        assertEquals(java.sql.RowId.class.getName(), mapping.getType(Types.ROWID));
        assertEquals(java.sql.SQLXML.class.getName(), mapping.getType(Types.SQLXML));
        assertEquals(java.sql.ResultSet.class.getName(), mapping.getType(Types.REF_CURSOR));
        assertEquals(URL.class.getName(), mapping.getType(Types.DATALINK));
        assertEquals(Object.class.getName(), mapping.getType(Types.OTHER));
    }

    @Test
    public void approximateNumericJdbcFallbackUsesRecommendedObjectMappings() {
        assertEquals(Float.class.getName(), mapping.getType(Types.REAL));
        assertEquals(Double.class.getName(), mapping.getType(Types.FLOAT));
        assertEquals(Double.class.getName(), mapping.getType(Types.DOUBLE));
    }

    @Test
    public void classMappingsCoverMaterializedLobsAndJdbcExtensionValues() {
        assertEquals("byte[]", mapping.getType(byte[].class.getName()));
        assertEquals("byte[]", mapping.getType(java.sql.Blob.class.getName()));
        assertEquals(String.class.getName(), mapping.getType(java.sql.Clob.class.getName()));
        assertEquals(String.class.getName(), mapping.getType(java.sql.NClob.class.getName()));
        assertEquals(UUID.class.getName(), mapping.getType(UUID.class.getName()));
        assertEquals(URL.class.getName(), mapping.getType(URL.class.getName()));
    }

    @Test
    public void customMappingsSupportCanonicalArrayNamesAndRemoval() {
        mapping.addMapping(String.class, byte[].class);
        assertEquals("byte[]", mapping.getType(String.class.getName()));
        mapping.removeMapping(String.class);
        assertNull(mapping.getType(String.class.getName()));

        mapping.addMapping("source.Type", "target.Type");
        assertEquals("target.Type", mapping.getType("source.Type"));
        mapping.removeMapping("source.Type");
        assertNull(mapping.getType("source.Type"));

        mapping.addMapping(Types.OTHER, "custom.Json");
        assertEquals("custom.Json", mapping.getType(Types.OTHER));
        mapping.removeMapping(Types.OTHER);
        assertNull(mapping.getType(Types.OTHER));
    }

    @Test
    public void targetClassWithoutCanonicalNameIsRejected() {
        Class<?> anonymousClass = new Object() { }.getClass();

        try {
            mapping.addMapping(String.class, anonymousClass);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException actual) {
            assertTrue(actual.getMessage().contains("canonical name"));
        }
    }
}
