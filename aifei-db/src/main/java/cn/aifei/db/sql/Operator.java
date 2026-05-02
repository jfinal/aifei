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

import java.util.HashMap;
import java.util.Map;

/**
 * where、and 子句中使用的操作符，支持以下操作符：
 *   =
 *   !=
 *   <>
 *   >
 *   >=
 *   <
 *   <=
 *   between
 *   not between
 *   in
 *   not in
 *   is null
 *   is not null
 *   like
 *   not like
 *   contains
 *   notContains
 *   startsWith
 *   endsWith
 *
 * 其中 is null 与 is not null 操作符没有参数，其它操作符均只有 1 个参数。
 */
public enum Operator {

    EQUAL("="),
    NOT_EQUAL("!="),

    GREATER(">"),
    GREATER_OR_EQUAL(">="),

    LESS("<"),
    LESS_OR_EQUAL("<="),

    IN("IN"),
    NOT_IN("NOT IN"),

    BETWEEN("BETWEEN"),
    NOT_BETWEEN("NOT BETWEEN"),

    IS_NULL("IS NULL"),
    IS_NOT_NULL("IS NOT NULL"),

    LIKE("LIKE", LikeMode.BOTH),
    NOT_LIKE("NOT LIKE", LikeMode.BOTH),

    // like 扩展操作符
    CONTAINS("contains", "LIKE", LikeMode.BOTH),
    NOT_CONTAINS("notContains", "NOT LIKE", LikeMode.BOTH),
    STARTS_WITH("startsWith", "LIKE", LikeMode.RIGHT),
    ENDS_WITH("endsWith", "LIKE", LikeMode.LEFT);

    private final String key;
    private final String sql;
    private final LikeMode likeMode;

    static final Map<String, Operator> cache = createCache();

    private static Map<String, Operator> createCache() {
        Map<String, Operator> ret = new HashMap<>();
        for (Operator op : values()) {
            ret.put(op.key, op);
            ret.put(op.key.toLowerCase(), op);
        }
        ret.put("<>", NOT_EQUAL);
        return ret;
    }

    Operator(String sql) {
        this(sql, sql, LikeMode.NONE);
    }

    Operator(String sql, LikeMode likeMode) {
        this(sql, sql, likeMode);
    }

    Operator(String key, String sql, LikeMode likeMode) {
        this.key = key;
        this.sql = sql;
        this.likeMode = likeMode;
    }

    /**
     * 参数 key 支持全大小与全小写字符，但不支持前后空格。
     */
    public static Operator from(String key) {
        return cache.get(key);
    }

    public String sql() {
        return sql;
    }

    /**
     * 对 like、not like、contains、notContains、startsWith、endsWith 参数值做包装
     */
    public Object toLikeValue(Object value) {
        // if (value == null || likeMode == LikeMode.NONE) {
        //     return value;
        // }
        return likeMode.wrap(value.toString());
    }

    @Override
    public String toString() {
        return sql;
    }

    public enum LikeMode {
        NONE {
            @Override
            public String wrap(String str) {
                return str;
            }
        },
        BOTH {
            @Override
            public String wrap(String str) {
                return "%" + str + "%";
            }
        },
        LEFT {
            @Override
            public String wrap(String str) {
                return "%" + str;
            }
        },
        RIGHT {
            @Override
            public String wrap(String str) {
                return str + "%";
            }
        };
        public abstract String wrap(String str);
    }
}


