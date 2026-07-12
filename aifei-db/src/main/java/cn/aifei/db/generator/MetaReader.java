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

import cn.aifei.db.dialect.Dialect;
import cn.aifei.db.dialect.OracleDialect;
import cn.aifei.enjoy.util.StrUtil;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * MetaReader 读取数据库 table meta 信息
 */
public class MetaReader {

    protected TypeMapping typeMapping = new TypeMapping();
    protected FieldToAttr fieldToAttr = new FieldToAttr.FieldToAttrImpl();

    protected boolean readView = false;                 // 是否读取视图 view
    protected boolean readFieldRemarks = true;          // 是否读取字段备注
    protected boolean readFieldAutoIncrement = false;   // 是否读取字段的自增属性

    protected Predicate<String> tableFilter = null;
    protected Predicate<String> tableSkip = null;
    protected Set<String> whitelist = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    protected Set<String> blacklist = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * 获取类型映射，可进行映射的修改与添加
     *
     * <pre>
     *  例子：
     *  new Generator(new MysqlDialect(), dataSource, modelPackage, modelPath)
     *      .configMetaReader(mr -> {
     *          mr.getTypeMapping().addMapping(...);
     *       })
     *       .generate();
     * </pre>
     */
    public TypeMapping getTypeMapping() {
        return typeMapping;
    }

    /**
     * 配置类型映射
     */
    public MetaReader setTypeMapping(TypeMapping typeMapping) {
        this.typeMapping = typeMapping;
        return this;
    }

    /**
     * 配置 fieldName 到 attrName 转换函数
     */
    public MetaReader setFieldToAttr(FieldToAttr fieldToAttr) {
        this.fieldToAttr = fieldToAttr;
        return this;
    }

    /**
     * 配置是否读取视图 view
     */
    public MetaReader setReadView(boolean readView) {
        this.readView = readView;
        return this;
    }

    /**
     * 配置是否读取字段备注
     */
    public MetaReader setReadFieldRemarks(boolean readFieldRemarks) {
        this.readFieldRemarks = readFieldRemarks;
        return this;
    }

    /**
     * 配置是否读取字段的自增属性
     */
    public MetaReader setReadFieldAutoIncrement(boolean readFieldAutoIncrement) {
        this.readFieldAutoIncrement = readFieldAutoIncrement;
        return this;
    }

    /**
     * lambda 实现 table 过滤。用于待处理 table 数量比较少的场景
     */
    public MetaReader filter(Predicate<String> tableFilter) {
        this.tableFilter = tableFilter;
        return this;
    }

    /**
     * lambda 实现 table 跳过/忽略。用于被忽略 table 数量比较少的场景
     */
    public MetaReader skip(Predicate<String> tableSkip) {
        this.tableSkip = tableSkip;
        return this;
    }

    /**
     * 添加要生成的 table 到白名单。白名单可实现只针对指定 table 生成代码
     * 注意：白名单优先于黑名单。白名单与黑名单并集必须为空
     */
    public MetaReader addWhitelist(String... tables) {
        for (String table : tables) {
            table = table.trim();
            if (blacklist.contains(table)) {
                throw new IllegalArgumentException("黑名单中已经存在的 table 不能加入白名单 -> " + table);
            }
            whitelist.add(table);
        }
        return this;
    }

    public MetaReader removeWhitelist(String table) {
        whitelist.remove(table.trim());
        return this;
    }

    /**
     * 添加要排除的 table 到黑名单。黑名单可实现过滤指定 table
     * 注意：白名单优先于黑名单。白名单与黑名单并集必须为空
     */
    public MetaReader addBlacklist(String... tables) {
        for (String table : tables) {
            table = table.trim();
            if (whitelist.contains(table)) {
                throw new IllegalArgumentException("白名单中已经存在的 table 不能加入黑名单 -> " + table);
            }
            blacklist.add(table);
        }
        return this;
    }

    public MetaReader removeBlacklist(String table) {
        blacklist.remove(table.trim());
        return this;
    }

    /**
     * 处理优先级为 whitelist、tableFilter、blacklist、tableSkip，逻辑如下：
     * 1: 若存在白名单，则根据 table 是否在白名单之内决定是否处理。
     *    注意：只要白名单中存在数据，则不会进入后续流程
     * <p>
     * 2: 若存在 tableFilter，则根据 table 是否被选中决定是否被处理。
     *    注意：只要存在 tableFilter，则不会进入后续流程
     * <p>
     * 3: 跳过黑名单中包含的 table，未跳过则进入下一个流程
     * <p>
     * 4: 跳过 tableSkip 选中的 table，未跳过则进入下一个流程
     */
    private boolean shouldProcess(String table) {
        // 若存在白名单，则根据 table 是否在白名单之内决定是否处理
        if (!whitelist.isEmpty()) {
            return whitelist.contains(table);
        }

        // 若存在 tableFilter，则根据 table 是否被选中决定是否被处理
        if (tableFilter != null) {
            return tableFilter.test(table);
        }

        // 跳过黑名单中包含的 table
        if (blacklist.contains(table)) {
            return false;
        }

        // 跳过 tableSkip 选中的 table
        if (tableSkip != null && tableSkip.test(table)) {
            return false;
        }

        return true;
    }

    public List<TableInfo> read(Dialect dialect, DataSource dataSource) {
        Objects.requireNonNull(dialect, "dialect can not be null.");
        Objects.requireNonNull(dataSource, "dataSource can not be null.");

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            List<TableInfo> ret = new ArrayList<>();
            readTableInfo(connection, databaseMetaData, dialect, ret);
            readFieldInfo(connection, databaseMetaData, dialect, ret);
            return ret;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 不同数据库 databaseMetaData.getTables(...) 的 schemaPattern 参数意义不同
     * 1：oracle 数据库这个参数代表 databaseMetaData.getUserName()
     * 2：postgresql 数据库中需要在 jdbcUrl中配置 schemaPatter，例如：
     *   jdbc:postgresql://localhost:15432/djpt?currentSchema=public,sys,app
     *   最后的参数就是搜索schema的顺序，DruidPlugin 下测试成功
     * 3：开发者若在其它库中发现工作不正常，可通过继承 MetaReader 并覆盖此方法来实现功能
     */
    protected void readTableInfo(Connection connection, DatabaseMetaData databaseMetaData, Dialect dialect, List<TableInfo> ret) throws SQLException {
        String schemaPattern = dialect instanceof OracleDialect ? databaseMetaData.getUserName() : null;
        String[] types = readView ? new String[]{"TABLE", "VIEW"} : new String[]{"TABLE"};
        try (ResultSet rs = databaseMetaData.getTables(connection.getCatalog(), schemaPattern, null, types)) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME").trim();
                // 通过白名单、黑名单、filter、skip 机制挑选应该被处理的 table
                if (!shouldProcess(tableName)) {
                    System.out.println("Skip table :" + tableName);
                    continue;
                }

                // 读取主键
                String[] primaryKey = readPrimaryKey(connection, databaseMetaData, dialect, tableName);

                // 读取是否为 view
                boolean isView = "VIEW".equalsIgnoreCase(rs.getString("TABLE_TYPE"));

                // 跳过没有主键的 table。但没有主键的 view 保留，主键使用 "fake_id"
                if (primaryKey == null || primaryKey.length == 0) {
                    if (readView && isView) {
                        primaryKey = new String[]{"fake_id"};       // primaryKey = dialect.getDefaultPrimaryKey();
                        System.out.println("Set primaryKey \"" + String.join(",", primaryKey) + "\" for " + tableName);
                    } else {
                        System.err.println("Skip table " + tableName + " because there is no primary key");
                        continue;
                    }
                }

                ret.add(new TableInfo(tableName, primaryKey, rs.getString("REMARKS"), isView));
            }
        }
    }

    protected String[] readPrimaryKey(Connection connection, DatabaseMetaData databaseMetaData, Dialect dialect, String tableName) throws SQLException {
        try (ResultSet rs = databaseMetaData.getPrimaryKeys(connection.getCatalog(), null, tableName)) {
            List<String> ret = new ArrayList<>(2);
            String lastValue = "";
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                // 避免 oracle 驱动的 bug 生成重复主键，如：ID,ID
                if (!lastValue.equals(columnName) && StrUtil.notBlank(columnName)) {
                    ret.add(columnName.trim());
                    lastValue = columnName.trim();
                }
            }
            return ret.toArray(new String[0]);
        }
    }

    /**
     * 文档参考：
     * http://dev.mysql.com/doc/connector-j/en/connector-j-reference-type-conversions.html
     *
     * JDBC 与时间有关类型转换规则，mysql 类型到 java 类型如下对应关系：
     * DATE				java.sql.Date
     * DATETIME			java.time.LocalDateTime
     * TIMESTAMP[(M)]	java.sql.Timestamp
     * TIME				java.sql.Time
     *
     * 对数据库的 DATE、DATETIME、TIMESTAMP、TIME 四种类型注入 new java.util.Date()对象保存到库以后可以达到“秒精度”
     * 为了便捷性，getter、setter 方法中对上述四种字段类型采用 java.util.Date，可通过定制 TypeMapping 改变此映射规则
     */
    protected void readFieldInfo(Connection connection, DatabaseMetaData databaseMetaData, Dialect dialect, List<TableInfo> ret) throws SQLException {
        for (TableInfo tableInfo : ret) {
            Map<String, String> fieldToRemarks = readFieldRemarks(connection, databaseMetaData, tableInfo.name);

            String sql = dialect.queryTableInfo(tableInfo.name);
            try (Statement stm = connection.createStatement(); ResultSet rs = stm.executeQuery(sql)) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    // 获取 fieldName
                    String fieldName = rsmd.getColumnName(i).trim();   // getColumnName 返回字段真名而并非 as 指定的别名

                    // getColumnClassName() 表示 getObject() 实际返回的 Java 类型，比通用 JDBC 类型更精确，故优先使用
                    // 类名未命中映射时，再使用 getColumnType() 返回的 JDBC 类型兜底
                    String fieldClassName = rsmd.getColumnClassName(i);
                    String javaType = typeMapping.getType(fieldClassName);
                    if (javaType == null) {
                        int type = rsmd.getColumnType(i);
                        javaType = typeMapping.getType(type);   // 通过 int 型 type 再取一次

                        if (javaType == null) {
                            javaType = Object.class.getName();
                        }
                    }

                    javaType = handleJavaType(dialect, javaType, rsmd, i);

                    // 获取 attrName
                    String attrName = fieldToAttr.convert(dialect, fieldName);

                    // 获取 fieldRemarks
                    String fieldRemarks = null;
                    if (readFieldRemarks && fieldToRemarks.containsKey(fieldName)) {
                        fieldRemarks = fieldToRemarks.get(fieldName);
                    }

                    // 获取 fieldAutoIncrement
                    Boolean fieldAutoIncrement = null;
                    if (readFieldAutoIncrement) {
                        fieldAutoIncrement = rsmd.isAutoIncrement(i);
                    }

                    // 移除包名前缀 "java.lang."
                    if (javaType.startsWith("java.lang.")) {
                        javaType = javaType.replaceFirst("java.lang.", "");
                    }

                    // 添加 FieldInfo 到 TableInfo
                    FieldInfo fieldInfo = new FieldInfo(fieldName, javaType, attrName, fieldRemarks, fieldAutoIncrement);
                    tableInfo.fieldInfoList.add(fieldInfo);
                }
            }
        }
    }

    /**
     * handleJavaType(...) 方法是用于处理 java 类型的回调方法，当 aifei-db 默认
     * 处理规则无法满足需求时，用户可以通过继承 MetaReader 并覆盖此方法定制自己的
     * 类型转换规则
     *
     * 当前实现只处理了 Oracle 数据库的 NUMBER 类型，根据精度与小数位数转换成 Integer、
     * Long、BigDecimal。其它数据库直接返回原值 typeStr
     *
     * Oracle 数据库 number 类型对应 java 类型：
     *  1：如果不指定number的长度，或指定长度 n > 18
     *     number 对应 java.math.BigDecimal
     *  2：如果number的长度在10 <= n <= 18
     *     number(n) 对应 java.lang.Long
     *  3：如果number的长度在1 <= n <= 9
     *     number(n) 对应 java.lang.Integer 类型
     *
     * 社区分享：《Oracle NUMBER 类型映射改进》https://jfinal.com/share/1145
     */
    protected String handleJavaType(Dialect dialect, String javaType, ResultSetMetaData rsmd, int column) throws SQLException {
        // 当前实现只处理 Oracle
        if (!(dialect instanceof OracleDialect)) {
            return javaType;
        }

        // 默认实现只处理 BigDecimal 类型
        if ("java.math.BigDecimal".equals(javaType)) {
            int scale = rsmd.getScale(column);			// 小数点右边的位数，值为 0 表示整数
            int precision = rsmd.getPrecision(column);	// 最大精度
            if (scale == 0) {
                if (precision <= 9) {
                    javaType = "java.lang.Integer";
                } else if (precision <= 18) {
                    javaType = "java.lang.Long";
                } else {
                    javaType = "java.math.BigDecimal";
                }
            } else {
                // 非整数都采用 BigDecimal 类型，需要转成 double 的可以覆盖并改写下面的代码
                javaType = "java.math.BigDecimal";
            }
        }

        return javaType;
    }

    // 获取字段 remarks 需使用 databaseMetaData.getColumns(...)
    private Map<String, String> readFieldRemarks(Connection connection, DatabaseMetaData databaseMetaData, String tableName) throws SQLException {
        Map<String, String> ret = new LinkedHashMap<>();
        if (readFieldRemarks) {
            try (ResultSet rs = databaseMetaData.getColumns(connection.getCatalog(), null, tableName, null)) {
                while (rs.next()) {
                    ret.put(rs.getString("COLUMN_NAME").trim(), rs.getString("REMARKS"));
                }
            }
        }
        return ret;
    }
}


