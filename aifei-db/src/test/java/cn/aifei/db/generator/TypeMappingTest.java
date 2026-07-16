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
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TypeMappingTest {

    private final TypeMapping mapping = new TypeMapping();

    @Test
    public void shortIsNormalizedToInteger() {
        assertEquals(Integer.class.getName(), mapping.getType(Short.class.getName()));
        assertEquals(Integer.class.getName(), mapping.getType(Types.SMALLINT));
    }

    @Test
    public void byteIsNormalizedToInteger() {
        assertEquals(Integer.class.getName(), mapping.getType(Byte.class.getName()));
        assertEquals(Integer.class.getName(), mapping.getType(Types.TINYINT));
    }

    @Test
    public void offsetTypesRequireMatchingColumnClassName() {
        assertEquals(OffsetDateTime.class.getName(), mapping.getType(OffsetDateTime.class.getName()));
        assertEquals(OffsetTime.class.getName(), mapping.getType(OffsetTime.class.getName()));

        // JDBC 类型本身不能证明 getObject() 已返回 Offset 类型。
        assertNull(mapping.getType(Types.TIMESTAMP_WITH_TIMEZONE));
        assertNull(mapping.getType(Types.TIME_WITH_TIMEZONE));
    }
}
