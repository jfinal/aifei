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

package cn.aifei.db.ext;

import cn.aifei.db.factory.IdSqlFactory;
import cn.aifei.db.util.Murmur3Util;
import java.nio.charset.StandardCharsets;

/**
 * 从 idSql 中拆分出 id 与 sql，如果 id 不存在则使用 sql 值构造出一个 id 值。
 * id 用于缓存 sql 生成的 enjoy Template 对象，官方给出三个实现：
 *
 *   1：MurmurFactory
 *   2：SqlSelfFactory
 *   3：NullFactory
 */
public class IdSqlFactories {

    /**
     * 将 sql 自身作为 sqlId 生成
     */
    public static class SqlSelfFactory implements IdSqlFactory {
        @Override
        public String generateId(String sql) {
            return sql;
        }
    }

    /**
     * 永久返回 null 值，即不启用缓存，而是每次调用都重新解析（不推荐）
     */
    public static class NullFactory implements IdSqlFactory {
        @Override
        public String generateId(String sql) {
            return null;
        }
    }

    /**
     * 使用 murmur3 128 hash 算法生成 sqlId
     */
    public static class MurmurFactory implements IdSqlFactory {

        static final long MURMUR_SEED = 0x7f3a21eaL;    // 0xB0F57EE3;
        private int minLength = 512;

        public MurmurFactory() {}

        public MurmurFactory(int minLength) {
            this.minLength = minLength;
        }

        @Override
        public String generateId(String sql) {
            if (sql.length() > minLength) {
                byte[] sqlBytes = sql.getBytes(StandardCharsets.UTF_8);
                long[] longs = Murmur3Util.hash_x64_128(sqlBytes, sqlBytes.length, MURMUR_SEED);
                return longs[0] + ":" + longs[1];
            } else {
                // 未超过最小长度返回 sql 自身作为 sqlId
                return sql;
            }
        }
    }
}



