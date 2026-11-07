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

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PageTest {

    @Test
    public void calculatesEmptyExactAndPartialPages() {
        assertEquals(0L, page(0, 10).getTotalPages());
        assertEquals(1L, page(1, 10).getTotalPages());
        assertEquals(10L, page(100, 10).getTotalPages());
        assertEquals(11L, page(101, 10).getTotalPages());
    }

    @Test
    public void totalPagesCanCrossIntegerMaxValue() {
        assertEquals(2_147_483_647L, page(2_147_483_647L, 1).getTotalPages());
        assertEquals(2_147_483_648L, page(2_147_483_648L, 1).getTotalPages());
        assertEquals(2_147_483_648L, page(4_294_967_295L, 2).getTotalPages());

        Page<String> page = page(3_000_000_000L, 1);
        page.setPageNum(Integer.MAX_VALUE);
        assertFalse(page.lastPage());
    }

    @Test
    public void longMaxValueDoesNotOverflowWhenRoundingUp() {
        assertEquals(Long.MAX_VALUE, page(Long.MAX_VALUE, 1).getTotalPages());
        assertEquals(4_611_686_018_427_387_904L, page(Long.MAX_VALUE, 2).getTotalPages());
        assertEquals(922_337_203_685_477_581L, page(Long.MAX_VALUE, 10).getTotalPages());
        assertEquals(4_294_967_299L, page(Long.MAX_VALUE, Integer.MAX_VALUE).getTotalPages());
    }

    @Test
    public void precomputedTotalPagesRetainsLongRange() {
        Page<String> page = new Page<>(1, 1, Long.MAX_VALUE, Long.MAX_VALUE, Collections.emptyList());

        assertEquals(Long.MAX_VALUE, page.getTotalPages());
        assertTrue(page.firstPage());
        assertFalse(page.lastPage());

        page.setTotalPages(3_000_000_000L);
        assertEquals(3_000_000_000L, page.getTotalPages());
    }

    private Page<String> page(long totalRows, int pageSize) {
        return new Page<>(1, pageSize, totalRows, Collections.emptyList());
    }
}
