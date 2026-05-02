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

package cn.aifei.db.transaction;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 事务隔离级别
 */
public enum Isolation {

    // 数值分别为：0、1、2、4、8
    NONE(Connection.TRANSACTION_NONE),
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    public final int level;

    private static final Map<Integer, Isolation> cache = Arrays.stream(values())
            .collect(Collectors.toMap(k -> k.level, v -> v));

    Isolation(int level) {
        this.level = level;
    }

    public static Isolation from(int level) {
        Isolation ret = cache.get(level);
        if (ret != null) {
            return ret;
        } else {
            throw new IllegalArgumentException("Unknown transaction isolation level: " + level);
        }
    }
}


