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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SqlPara 封装查询使用的 sql 与参数
 */
public class SqlPara implements Serializable {

    private static final long serialVersionUID = -8586448059592782381L;

    String id;
    String sql;
    List<Object> paraList;

    boolean enjoySql = false;

    public SqlPara() {}

    public SqlPara(String sql, List<Object> paraList) {
        this.sql = sql;
        this.paraList = paraList;
    }

    public SqlPara(String sql, Object... paraArray) {
        this.sql = sql;
        this.paraList = Arrays.asList(paraArray);
    }

    /**
     * 模板解析需要调用此方法写入 sqlId，其它情况如 Db.sql(...)、Db.sqlById(...) 直接传入
     */
    public SqlPara setId(String id) {
        this.id = id;
        return this;
    }

    public String getId() {
        return id != null ? id : sql;
    }

    public SqlPara setParaArray(Object[] paras) {
        if (paras != null && paras.length > 0) {
            if (paraList != null) {
                paraList.clear();
            } else {
                paraList = new ArrayList<>(paras.length);
            }

            for (Object para : paras) {
                paraList.add(para);
            }
        }
        return this;
    }

    public SqlPara setSql(String sql) {
        this.sql = sql;
        return this;
    }

    public String getSql() {
        return sql;
    }

    public SqlPara addPara(Object para) {
        if (paraList == null) {
            paraList = new ArrayList<>();
        }
        paraList.add(para);
        return this;
    }

    public List<Object> getPara() {
        return paraList;
    }

    public SqlPara setPara(List<Object> paraList) {
        this.paraList = paraList;
        return this;
    }

    public Object[] getParaArray() {
        if (paraList == null || paraList.isEmpty()) {
            return DbKit.EMPTY_OBJECT_ARRAY;
        } else {
            return paraList.toArray(DbKit.EMPTY_OBJECT_ARRAY);
        }
    }

    /**
     * 标记是否为 enjoy sql。Db.sql(String, Object...) 既支持纯 sql 又支持 enjoy sql
     *
     * 后续考虑优化：纯 sql 不走 enjoy engine
     */
    public SqlPara setEnjoySql(boolean enjoySql) {
        this.enjoySql = enjoySql;
        return this;
    }

    public boolean isEnjoySql() {
        return enjoySql;
    }

    /**
     * 纯 sql 场景，添加参数到 SqlPara
     */
    public void setParasIfNotEnjoySql(Object[] paras) {
        if (!enjoySql) {
            setParaArray(paras);
        }
    }

    public SqlPara clear() {
        id = null;
        sql = null;
        enjoySql = false;
        if (paraList != null) {
            paraList.clear();
        }
        return this;
    }

    public String toString() {
        return "Sql: " + sql + "\nPara: " + paraList + "\n\nId: " + id + ", EnjoySql: " + enjoySql;
    }

    // ---------------------------------------------------------------------------------------
    // SqlPara 生成缓存 key 接口如下：-----------------------------------------------------------

    /**
     * 例子：
     *  SqlPara.keyGenerator((prefix, sqlId, paraList) -> {
     *     int prefixLen = (prefix != null ? prefix.length() : 0);
     *     int sqlIdLen = (sqlId != null ? sqlId.length() : 0);
     *     int paraNum = (paraList != null ? paraList.size() : 0);
     *     StringBuilder ret = new StringBuilder(prefixLen + sqlIdLen + 16 + paraNum * 16 );
     *     if (prefix != null) {    // 支持前缀，方便 redis 对 key 进行管理
     *         ret.append(prefix);
     *     }
     *     if (sqlId != null) {     // 支持 generateKeyByPara，该方法将对 sqlId 传入 null 值
     *         if (sqlId.length() > 300) {
     *             .... 使用 Murmur3Util 生成
     *         } else {
     *             ret.append(sqlId);
     *         }
     *     }
     *     if (paraList != null) {
     *         for (Object p : paraList) {
     *             ret.append(p);
     *         }
     *     }
     *     return ret.toString();
     *  });
     */
    @FunctionalInterface
    public interface KeyGenerator {
        // 约定 sqlId 参数传入 null 时，sqlId 部分不参与生成
        String generate(String prefix, String sqlId, List<Object> paraList);
    }

    private static KeyGenerator keyGenerator;

    public synchronized static void keyGenerator(KeyGenerator keyGenerator) {
        if (keyGenerator == null) {
            throw new IllegalArgumentException("The keyGenerator of SqlPara can not be null.");
        }
        if (SqlPara.keyGenerator != null) {
            throw new IllegalStateException("The keyGenerator of SqlPara has already been set.");
        }
        SqlPara.keyGenerator = keyGenerator;
    }

    /**
     * 根据 SqlPara 对象生成 key，用于缓存 paginate 查询的 totalRows 值
     * 生成规则可以考虑：将 paraList 生成 json 字符串（长度小于 1K 的直接使用，否则考虑）
     */
    public String generateKey(String prefix) {
        if (keyGenerator == null) {
            throw new IllegalStateException("The keyGenerator of SqlPara is not set.");
        }
        return keyGenerator.generate(prefix, getId(), getPara());
    }

    public String generateKey() {
        return generateKey(null);
    }

    /**
     * 指定 key。SqlPara.paraList 参与生成最终的 key 值
     */
    public String generateKeyByPara(String key) {
        if (keyGenerator == null) {
            throw new IllegalStateException("The keyGenerator of SqlPara is not set.");
        }
        // 约定此处 sqlId 参数传入 null，即 sqlId 部分不参与生成
        return keyGenerator.generate(key, null, getPara());
    }

    public String generateKeyByPara() {
        return generateKeyByPara(null);
    }
}


