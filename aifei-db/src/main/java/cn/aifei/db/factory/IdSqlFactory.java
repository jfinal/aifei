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

package cn.aifei.db.factory;

/**
 * 将 id 与 sql 从 idSql 中拆分出来，id 部分用于缓存该 sql，sql 用于操作数据库。
 * 如果 idSql 中不存在 id 则使用 sql 值构造一个 id 值。
 * <p>
 * 指定 sql 的 id 用于避免超长 sql 生成 id，提升性能，对于多数 sql 不必指定 id，
 * IdSqlFactory 默认实现类会生成 id 用于 sql 模板对象的缓存
 *
 * <pre>
 * 例子：
 *    Db.sql("|findUser| select * from user where id = ?", 123).find();
 *    以上 Db.sql(...) 方法参数 idSql，从中拆分出来的 id 值为 "findUser"，sql 值为 " select * from user where id = ?"
 * </pre>
 */
@FunctionalInterface
public interface IdSqlFactory {

    /**
     * 根据 sql 生成 id
     */
    String generateId(String sql);

    // 分隔 id 与 sql 的字符，其它字符仅建议: / -
    char idStartChar = '|';
    char idEndChar = '|';

    /**
     * 获取 id 开始字符
     */
    default char getIdStartChar() {
        return idStartChar;
    }

    /**
     * 获取 id 结束字符
     */
    default char getIdEndChar() {
        return idEndChar;
    }

    default String[] splitIdAndSql(String idAndSql) {
        String[] ret = new String[2];

        char ch;
        int idStart = -1;
        int length = idAndSql.length();
        char idStartChar = getIdStartChar();
        for (int i = 0; i < length; i++) {
            ch = idAndSql.charAt(i);
            if (ch == idStartChar) {
                idStart = i + 1;        // 找到 idStartChar
                break;
            }

            // 需判断 idEndChar 前方只允许为空白字符，避免未指定 id 时扫描整个字符串
            if (ch != ' ' && ch != '\t' && ch != '\n') {
                break;
            }
        }

        if (idStart != -1) {
            int idEnd = -1;
            char idEndChar = getIdEndChar();
            for (int i = idStart; i < length; i++) {
                ch = idAndSql.charAt(i);
                if (ch == idEndChar) {
                    idEnd = i;        // 找到 idEndChar
                    break;
                }

                // idEndChar 只允许与 idStartChar 出现在同一行。如果碰到回车换行则认定未找到 idEndChar
                if (ch == '\n') {
                    throw new IllegalArgumentException("idEndChar not found in the same line of the idStartChar");
                }
            }

            if (idEnd != -1 && idEnd > idStart /* && idEnd + 1 < length */) {
                ret[0] = idAndSql.substring(idStart, idEnd).trim();
                ret[1] = idAndSql.substring(idEnd + 1, length);
                return ret;
            }
        }

        // 未能从 idAndSql 分离出 id，调用 generateId 生成 id 值
        ret[0] = generateId(idAndSql);
        ret[1] = idAndSql;
        return ret;
    }
}




