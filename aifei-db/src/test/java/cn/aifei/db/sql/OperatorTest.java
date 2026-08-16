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

package cn.aifei.db.sql;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class OperatorTest {

    @Test
    public void ofResolvesCanonicalAliasesWithoutTrimmingInput() {
        assertSame(Operator.EQUAL, Operator.of("="));
        assertSame(Operator.NOT_EQUAL, Operator.of("!="));
        assertSame(Operator.NOT_EQUAL, Operator.of("<>"));
        assertSame(Operator.IN, Operator.of("IN"));
        assertSame(Operator.IN, Operator.of("in"));
        assertSame(Operator.NOT_CONTAINS, Operator.of("notContains"));
        assertSame(Operator.NOT_CONTAINS, Operator.of("notcontains"));

        assertNull(Operator.of(" IN"));
        assertNull(Operator.of("In"));
        assertNull(Operator.of("unknown"));
        assertNull(Operator.of(null));
    }

    @Test
    public void likeOperatorsWrapValuesAccordingToTheirMode() {
        assertEquals("value", Operator.EQUAL.toLikeValue("value"));
        assertEquals("%value%", Operator.LIKE.toLikeValue("value"));
        assertEquals("%value%", Operator.CONTAINS.toLikeValue("value"));
        assertEquals("value%", Operator.STARTS_WITH.toLikeValue("value"));
        assertEquals("%value", Operator.ENDS_WITH.toLikeValue("value"));
    }
}
