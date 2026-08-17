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
 * MetaReader 与 Dialect 默认实现的读取方式保持一致：DATE 与 TIMESTAMP
 * 分别通过 ResultSet.getDate(i) 和 getTimestamp(i) 读取，因此使用
 * java.sql.Date 与 java.sql.Timestamp 作为类名映射键；其它类型使用
 * ResultSetMetaData.getColumnClassName(i) 返回的类名。类名映射未命中时，
 * 再使用 ResultSetMetaData.getColumnType(i) 返回的 JDBC 类型兜底；
 * 两次均未命中时默认使用 java.lang.Object。
 * 若自定义 Dialect 覆盖 readColumnValue(...) 并改变 DATE、TIMESTAMP 或其它
 * JDBC 类型的读取方式，必须同时覆盖 resolveColumnValueClassName(...)，
 * 使类名映射的源类型推断与运行时实际读取路径保持一致。
 *
 * <p>
 * 带时区类型只按 getColumnClassName(i) 返回的 OffsetDateTime/OffsetTime 类名映射，
 * 不按 TIMESTAMP_WITH_TIMEZONE/TIME_WITH_TIMEZONE 做 JDBC 类型兜底。部分驱动虽然
 * 报告这两个 JDBC 类型，getObject(i) 却可能返回厂商专用对象；如果兜底生成 Offset 字段，
 * 则生成的 getter 会在运行时发生 ClassCastException。
 *
 * <p>
 * 默认将 java.sql.Timestamp 映射为 java.util.Date，默认生成的 getter 会调用
 * getDate(String) 并原样返回 Timestamp 对象，因此保留具体类型及纳秒。由 getObject(i)
 * 返回的 LocalDateTime 也映射为 java.util.Date，并由 TypeConverter 转换成
 * Timestamp。DATE 保留 java.sql.Date 类型；其它 getObject(i) 路径报告的
 * LocalDate/LocalTime 保持原类型，TIME 报告 java.sql.Time 时也保持原类型。
 * 这些映射不隐式补入缺失的日期或时间，也不将 LocalTime 降格为 java.sql.Time。
 * 可通过 addMapping(...) 和 removeMapping(...) 调整默认映射规则。
 */
public class TypeMapping {

	protected Map<String, String> classNameToJavaType = new HashMap<String, String>(64) {{
		// 普通 java.util.Date 表示毫秒精度的时间点，保持原类型
		put("java.util.Date", "java.util.Date");

		// Dialect 默认对 TIMESTAMP 使用 getTimestamp() 读取；生成类型为 java.util.Date，运行时值仍为 Timestamp
		put("java.sql.Timestamp", "java.util.Date");

		/*
		 * DATE/TIMESTAMP 的默认路径已由 MetaReader 按 getDate()/getTimestamp()
		 * 的返回类型处理。以下映射适用于其它通过 getObject() 读取、并在元数据中
		 * 明确报告 Java 8 日期时间类的字段。LocalDateTime 由生成 getter 通过
		 * TypeConverter 转换成 Timestamp，按 JDBC 本地日期时间语义保留字段与纳秒。
		 */
		put("java.time.LocalDateTime", "java.util.Date");

		// --------------------------------------------------------------------
		// --------------------------------------------------------------------

		// Dialect 默认对 DATE 使用 getDate() 读取，生成类型与运行时值保持一致
		put("java.sql.Date", "java.sql.Date");

		// Dialect 默认对 TIME 使用 getObject() 读取；元数据报告 java.sql.Time 时保持原类型
		put("java.sql.Time", "java.sql.Time");

		// LocalTime 保持与 getObject() 返回值一致，避免生成 java.sql.Time getter 后强转失败
		// 不将 LocalTime 降格转换为 java.sql.Time，以免丢失纳秒精度
		put("java.time.LocalTime", "java.time.LocalTime");
		put("java.time.LocalDate", "java.time.LocalDate");

		// binary, varbinary, tinyblob, blob, mediumblob, longblob
		// qjd project: print_info.content varbinary(61800);
		put("[B", "byte[]");
		put("java.sql.Blob", "byte[]");

		put("java.sql.Clob", "java.lang.String");
		put("java.sql.NClob", "java.lang.String");

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

		// Short is normalized to Integer to keep generated models stable across
		// JDBC drivers and driver versions
		put("java.lang.Short", "java.lang.Integer");

		// Byte is normalized to Integer for the same reason as Short
		put("java.lang.Byte", "java.lang.Integer");

		/*
		 * getColumnClassName() 与无类型参数的 getObject() 是 JDBC 规范中的配套契约。
		 * Dialect 默认不会根据 TIME_WITH_TIMEZONE/TIMESTAMP_WITH_TIMEZONE 强制转换，
		 * 因此只有驱动明确报告 Offset 类名时才能映射成对应的 Offset 类型。
		 */
		put("java.time.OffsetDateTime", "java.time.OffsetDateTime");
		put("java.time.OffsetTime", "java.time.OffsetTime");

		put("java.sql.Array", "java.sql.Array");
		put("java.sql.Struct", "java.sql.Struct");
		put("java.sql.Ref", "java.sql.Ref");
		put("java.sql.RowId", "java.sql.RowId");
		put("java.sql.SQLXML", "java.sql.SQLXML");
		put("java.sql.ResultSet", "java.sql.ResultSet");

		put("java.util.UUID", "java.util.UUID");
		put("java.net.URL", "java.net.URL");
	}};

	public void addMapping(Class<?> from, Class<?> to) {
		String javaType = to.getCanonicalName();
		if (javaType == null) {
			throw new IllegalArgumentException("The target type must have a canonical name: " + to.getName());
		}
		classNameToJavaType.put(from.getName(), javaType);
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
		// 类名映射未命中时的兜底类型：DATE/TIMESTAMP 对齐 Dialect 默认的类型化读取，
		put(Types.TIMESTAMP, java.util.Date.class.getName());

		// --------------------------------------------------------------------
		// --------------------------------------------------------------------

		put(Types.TIME, java.sql.Time.class.getName());
		put(Types.DATE, java.sql.Date.class.getName());

		/*
		 * 不要在此将 TIMESTAMP_WITH_TIMEZONE/TIME_WITH_TIMEZONE 兜底映射成 Offset 类型。
		 * 例如 H2 1.4 默认对 TIMESTAMP_WITH_TIMEZONE 返回
		 * org.h2.api.TimestampWithTimeZone，而 Dialect 默认保留 getObject() 的返回类型。
		 * 类名未命中时退回 Object，比生成一个无法安全强转的 Offset getter 更可靠。
		 */

		put(Types.TINYINT, Integer.class.getName());
		put(Types.SMALLINT, Integer.class.getName());
		put(Types.INTEGER, Integer.class.getName());

		put(Types.BIGINT, Long.class.getName());

		put(Types.NUMERIC, java.math.BigDecimal.class.getName());
		put(Types.DECIMAL, java.math.BigDecimal.class.getName());

		/*
		 * JDBC 对 ResultSet.getObject(...) 的推荐 Java 对象映射是：
		 * REAL -> Float，FLOAT -> Double，DOUBLE -> Double。
		 * JDBC FLOAT 不能按名称直觉等同于 Java float，请勿交换下面两个映射。
		 *
		 * JDBC 4.2 规范附录 B（Data Type Conversion Tables）：
		 * https://download.oracle.com/otndocs/jcp/jdbc-4_2-mrel2-spec/index.html
		 * Oracle JDBC 类型映射说明（8.3.8、8.3.10、8.9.3）：
		 * https://docs.oracle.com/javase/1.5.0/docs/guide/jdbc/getstart/mapping.html
		 *
		 * 这里仅在 getColumnClassName(...) 的类名映射未命中时兜底。数据库行为
		 * 偏离 JDBC 映射时，应由对应 Dialect.resolveColumnValueClassName(...)
		 * 按实际运行时返回类型处理，例如 SQLite 将 REAL 明确解析为 Double。
		 */
		put(Types.REAL, Float.class.getName());
		put(Types.FLOAT, Double.class.getName());
		put(Types.DOUBLE, Double.class.getName());

		put(Types.BIT, Boolean.class.getName());
		put(Types.BOOLEAN, Boolean.class.getName());

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
