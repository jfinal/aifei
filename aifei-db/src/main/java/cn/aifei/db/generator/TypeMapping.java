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

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * TypeMapping 用于配置数据库字段到生成代码 Java 类型的映射关系。
 *
 * <p>
 * MetaReader 优先使用 ResultSetMetaData.getColumnClassName(i) 返回的类名进行映射；
 * 未找到时，再使用 ResultSetMetaData.getColumnType(i) 返回的 JDBC 类型进行兜底映射；
 * 两次均未找到时默认使用 java.lang.String。
 *
 * <p>
 * 默认将日期、时间戳类型映射为 java.util.Date，TIME 类型映射为 java.sql.Time。
 * 可通过 addMapping(...) 和 removeMapping(...) 调整默认映射规则。
 */
public class TypeMapping {

	protected Map<String, String> classNameToJavaType = new HashMap<String, String>(32) {{
		// java.util.Date can not be returned
		// java.sql.Date, java.sql.Time, java.sql.Timestamp all extends java.util.Date so getDate can return the three types data
		put("java.util.Date", "java.util.Date");

		// date, year
		put("java.sql.Date", "java.util.Date");

		// time
		// put("java.sql.Time", "java.util.Date");
		// 生成器需要生成 java.sql.Time 类型的 getter/setter 方法，以便 getBean 能正常工作
		put("java.sql.Time", "java.sql.Time");

		// timestamp, datetime
		put("java.sql.Timestamp", "java.util.Date");

		// binary, varbinary, tinyblob, blob, mediumblob, longblob
		// qjd project: print_info.content varbinary(61800);
		put("[B", "byte[]");

		// ---------

		// varchar, char, enum, set, text, tinytext, mediumtext, longtext
		put("java.lang.String", "java.lang.String");

		// int, integer, tinyint, smallint, mediumint
		put("java.lang.Integer", "java.lang.Integer");

		// bigint
		put("java.lang.Long", "java.lang.Long");

		// real, double
		put("java.lang.Double", "java.lang.Double");

		// float
		put("java.lang.Float", "java.lang.Float");

		// bit
		put("java.lang.Boolean", "java.lang.Boolean");

		// decimal, numeric
		put("java.math.BigDecimal", "java.math.BigDecimal");

		// unsigned bigint
		put("java.math.BigInteger", "java.math.BigInteger");

		// short
		put("java.lang.Short", "java.lang.Short");

		// byte
		put("java.lang.Byte", "java.lang.Byte");

		// 新增 java 8 的三种时间类型
		// put("java.time.LocalDateTime", "java.time.LocalDateTime");
		// put("java.time.LocalDate", "java.time.LocalDate");
		// put("java.time.LocalTime", "java.time.LocalTime");

		/*
		 * 部分同学反馈使用原始的 Date 更常用，故默认使用原始 Date
		 * 需要调整的通过可通过在 Generator.configMetaReader 方法内调用 addTypeMapping(...) 来覆盖默认映射
		 *
		 * 也可以通过 removeMapping(...) 来清除默认映射，让 JDBC 自动处理映射关系
		 *
		 * 注意：mysql 8 版本会将 datetime 字段类型映射为 LocalDateTime
		 */
		put("java.time.LocalDateTime", "java.util.Date");
		put("java.time.LocalDate", "java.util.Date");
		put("java.time.LocalTime", "java.sql.Time");
	}};

	public void addMapping(Class<?> from, Class<?> to) {
		classNameToJavaType.put(from.getName(), to.getName());
	}

	public void addMapping(String from, String to) {
		classNameToJavaType.put(from, to);
	}

	public void removeMapping(Class<?> from) {
		classNameToJavaType.remove(from.getName());
	}

	public void removeMapping(String from) {
		classNameToJavaType.remove(from);
	}

	public String getType(String className) {
		return classNameToJavaType.get(className);
	}

	// ---------------------------------------------------------------------------------------

	protected Map<Integer, String> jdbcTypeToJavaType = new HashMap<Integer, String>(64) {{
		put(Types.TINYINT, Integer.class.getName());
		put(Types.SMALLINT, Integer.class.getName());
		put(Types.INTEGER, Integer.class.getName());

		put(Types.BIGINT, Long.class.getName());

		put(Types.NUMERIC, java.math.BigDecimal.class.getName());
		put(Types.DECIMAL, java.math.BigDecimal.class.getName());

		put(Types.REAL, Float.class.getName());
		put(Types.FLOAT, Double.class.getName());
		put(Types.DOUBLE, Double.class.getName());

		put(Types.BIT, Boolean.class.getName());
		put(Types.BOOLEAN, Boolean.class.getName());

		put(Types.DATE, java.util.Date.class.getName());
		put(Types.TIMESTAMP, java.util.Date.class.getName());
		put(Types.TIME, java.sql.Time.class.getName());
		put(Types.TIMESTAMP_WITH_TIMEZONE, java.time.OffsetDateTime.class.getName());
		put(Types.TIME_WITH_TIMEZONE, java.time.OffsetTime.class.getName());

		put(Types.BINARY, "byte[]");
		put(Types.VARBINARY, "byte[]");
		put(Types.LONGVARBINARY, "byte[]");
		put(Types.BLOB, "byte[]");

		put(Types.CHAR, String.class.getName());
		put(Types.VARCHAR, String.class.getName());
		put(Types.LONGVARCHAR, String.class.getName());
		put(Types.NCHAR, String.class.getName());
		put(Types.NVARCHAR, String.class.getName());
		put(Types.LONGNVARCHAR, String.class.getName());
		put(Types.CLOB, String.class.getName());
		put(Types.NCLOB, String.class.getName());

		put(Types.ARRAY, java.sql.Array.class.getName());
		put(Types.STRUCT, java.sql.Struct.class.getName());
		put(Types.REF, java.sql.Ref.class.getName());
		put(Types.ROWID, java.sql.RowId.class.getName());
		put(Types.SQLXML, java.sql.SQLXML.class.getName());
		put(Types.REF_CURSOR, java.sql.ResultSet.class.getName());

		put(Types.DATALINK, java.net.URL.class.getName());

		put(Types.NULL, Object.class.getName());
		put(Types.JAVA_OBJECT, Object.class.getName());
		put(Types.DISTINCT, Object.class.getName());
		put(Types.OTHER, Object.class.getName());
	}};

	public void addMapping(int from, String to) {
		jdbcTypeToJavaType.put(from, to);
	}

	public void removeMapping(int from) {
		jdbcTypeToJavaType.remove(from);
	}

	public String getType(int jdbcType) {
		return jdbcTypeToJavaType.get(jdbcType);
	}
}
