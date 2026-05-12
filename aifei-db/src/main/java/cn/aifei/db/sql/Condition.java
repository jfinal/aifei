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

import cn.aifei.db.core.SqlPara;
import cn.aifei.enjoy.TemplateException;
import cn.aifei.enjoy.expr.ast.Const;
import cn.aifei.enjoy.expr.ast.Expr;
import cn.aifei.enjoy.expr.ast.ExprList;
import cn.aifei.enjoy.expr.ast.Id;
import cn.aifei.enjoy.io.Writer;
import cn.aifei.enjoy.stat.Location;
import cn.aifei.enjoy.stat.ParseException;
import cn.aifei.enjoy.stat.Scope;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Condition 封装 #where 指令与 #and 指令参数
 *
 * <pre>
 * 一、Condition 设计：
 *
 *  0: 优雅不是规则多，而是规则少但覆盖大多数场景
 *
 *  1: Condition 用于 #where 指令与 #and 指令，指令格式如下：
 *        #where(field, operator, para)
 *        #and(field, operator, para)
 *     例外：操作符 IS NULL、IS NOT NULL 只有 field、operator 没有 para
 *
 *     field 与 para 名称可以不相同，例如:
 *        #and(create_time, '>=', createdTime)
 *     其中 created_time 对应数据库字段名，而 createdTime 对应客户端参数名，消灭了参数名转换字段名问题。
 *
 *  2: field 参数对应于 sql 中实际使用的 field，例如如下 sql 片段：
 *        where name = ?
 *
 *     以上 sql 片段中的 name 就是 #where(name, ...) 中的 name
 *
 *  3: operator 参数对应于 sql 中的操作符，例如如下 sql 片段：
 *        where name = ?
 *
 *     以上 sql 片段中的 '=' 就是 #where(name, '=',...) 中的 '='
 *
 *  4: para 参数对应于 Db.sql(String, Map)、Db.sqlById(String, Map) 方法中 Map 传递的参数：
 *        Kv filter = Kv.of("age", 18);
 *        String sql = "select * from user #where(age, '>', age)";
 *        Db.sql(sql, filter).find();
 *
 *     以上 #where 指令的第三个参数 age 对应 Kv.of("age", 18) 传递的 age 参数，注意这个参数名可任取，例如：
 *        Kv filter = Kv.of("newName", 18)
 *        指令中的参数名改为相同名称 newName 即可：#where(age, '>', newName)
 *
 *  5: 参数值常量用法：
 *        #where(enabled, '=', true) #and(state, '=', 0)
 *
 *     以上参数值 true、0 为常量用法。
 *
 *  6: 灵活用法，field 可不必局限于变量名，只要能生成合法 sql 即可，例如：
 *        Kv filter = Kv.of("x", 100);
 *        String sql = "select * from user #where('id + 1', '>', x)";
 *        Db.sql(sql, filter).find();
 *
 *     以上 field 为一个表达式而非字段名，最终生成了合法的 sql，可被正确执行。充分发挥想象力！
 *
 *  7: #where 指令在开始生成 sql 之前会先生成 "WHERE"，同理 #and 会先生成 "AND"。如果整个过程没有 sql 被生成
 *     将不会生成 "WHERE"、"AND"，确保在没有 where、and 子句被生成时 sql 仍然正确。
 *
 *
 * 二、Condition 是否生成 sql 的策略：
 *
 *  1: 需要参数值的操作符，如 =、!=、>、>= 等操作符，在参数值为 null 时不生成 sql。例如：
 *        filter: {
 *           nickname: null,
 *           age: null
 *        }
 *     以上的 nickname、age 值为 null，将不生成 sql。
 *
 *  2: 没有参数值的操作符如 IS NULL、IS NOT NULL，只要 field 存在就会生成 sql。如果不希望生成 sql 有两种方法，
 *     以 javascript 为例，一是不传递该 field，二是将该 field 的值置为 undefined。以后一种方法为例：
 *        filter: {
 *           nickname: undefined
 *        }
 *     以上的 nickname 值为 undefined，将不会生成 sql。（注意：即便是 null 值也会生成 sql）
 *
 *  3: BETWEEN 操作符需要其数组参数中的两个元素都不为 null，才会生成 sql。
 * </pre>
 */
public class Condition {

    String field;
    Operator operator;
    Expr para;
    Location location;

    /**
     * 构造方法中解析参数
     *
     * <pre>
     * 参数格式:
     *  1: 单参数操作符，如 =、!=
     *      (field, operator, para)
     *
     *  2: 零参数操作符，如 is null、is not null
     *      (field, operator)
     * </pre>
     */
    public Condition(ExprList exprList, String directive, Location location) {
        int len = exprList.length();
        if (len < 2 || len > 3) {
            throw new ParseException(directive + " requires 2 to 3 arguments, but got: " + len, location);
        }

        // 解析 field。field 支持字符串常量下的 Const 以及 Id 两种类型
        String field;
        Expr expr = exprList.getExpr(0);
        if (expr instanceof Id) {
            field = ((Id) expr).getId();
        } else if (expr instanceof Const && ((Const) expr).isStr()) { // Const 可支持 'u.id'
            field = ((Const) expr).getStr();    // field 在模板中书写，不必检测 sql 注入
        } else {
            throw new ParseException(directive + " first argument must be an identifier or string literal", location);
        }

        // 解析 operator
        Operator operator;
        expr = exprList.getExpr(1);
        if (!(expr instanceof Const) || !((Const) expr).isStr()) {
            throw new ParseException(directive + " second argument must be a string literal", location);
        }
        String opStr = ((Const) expr).getStr();
        operator = Operator.from(opStr);
        if (operator == null) {
            throw new ParseException(directive + " invalid operator (no extra spaces allowed): " + opStr, location);
        }

        int paraCount = len - 2;

        // 解析参数。零参数操作符
        if (operator == Operator.IS_NULL || operator == Operator.IS_NOT_NULL) {
            if (paraCount != 0) {
                throw new ParseException(operator.sql() + " requires 0 arguments, but got: " + paraCount, location);
            }
            init(field, operator, null, location);
            return;
        }

        // 解析参数。非零参数操作符统一接收一个 para 表达式
        if (paraCount != 1) {
            throw new ParseException(operator.sql() + " requires 1 argument, but got: " + paraCount, location);
        }
        init(field, operator, exprList.getExpr(2), location);
    }

    void init(String field, Operator operator, Expr para, Location location) {
        this.field = field;
        this.operator = operator;
        this.para = para;
        this.location = location;
    }

    void write(Writer writer, String str) {
        try {
            writer.write(str, 0, str.length());
        } catch (IOException e) {
            throw new TemplateException(e.getMessage(), location, e);
        }
    }

    public void generate(Scope scope, Writer writer, boolean[] firstCondition, SqlPara sqlPara) {
        // IS NULL、IS NOT NULL 生成只与 field 存在有关，与 value 是否为 null 值无关
        // javascript 前端可以将值赋为 undefined 避免生成 sql
        if (operator == Operator.IS_NULL || operator == Operator.IS_NOT_NULL) {
            if (scope.exists(field)) {
                generateConditionHead(writer, firstCondition);
                // write(writer, " ");
            }
            return;
        }

        // 其它所有操作符在 value 为 null 时不生成 sql
        Object value = para.eval(scope);
        if (value == null || (value instanceof String && ((String) value).isEmpty())) {
            return;
        }

        // 生成问号占位符及添加对应值到 sqlPara
        switch (operator) {
            case EQUAL:
            case NOT_EQUAL:
            case GREATER:
            case GREATER_OR_EQUAL:
            case LESS:
            case LESS_OR_EQUAL:
                generateConditionHead(writer, firstCondition);
                write(writer, " ?");
                sqlPara.addPara(value);
                break;
            case LIKE:
            case NOT_LIKE:
            case CONTAINS:
            case NOT_CONTAINS:
            case STARTS_WITH:
            case ENDS_WITH:
                generateConditionHead(writer, firstCondition);
                write(writer, " ?");
                Object likeValue = operator.toLikeValue(value);
                sqlPara.addPara(likeValue);
                break;
            case IN:
            case NOT_IN:
                generateInOrNotIn(writer, firstCondition, sqlPara, toInValueList(value));
                break;
            case BETWEEN:
            case NOT_BETWEEN:
                generateBetweenOrNotBetween(writer, firstCondition, sqlPara, toBetweenValueList(value));
                break;
            default:
                throw new TemplateException("Unsupported operator: " + operator.sql(), location);
        }
    }

    void generateConditionHead(Writer writer, boolean[] firstCondition) {
        if (firstCondition[0]) {
            firstCondition[0] = false;
            write(writer, "WHERE ");
        } else {
            write(writer, "AND ");
        }
        write(writer, field);
        write(writer, " ");
        write(writer, operator.sql());
    }

    List<Object> toInValueList(Object value) {
        return toValueList(value, false, true);
    }

    List<Object> toBetweenValueList(Object value) {
        return toValueList(value, true, false);
    }

    // 参数转成 List。IN、NOT IN 过滤内部 null 值，BETWEEN、NOT BETWEEN 保留 null 值
    List<Object> toValueList(Object value, boolean allowNull, boolean allowSingleValue) {
        if (value instanceof Collection) {
            Collection<?> c = (Collection<?>) value;
            if (!c.isEmpty()) {
                List<Object> ret = new ArrayList<>(c.size());
                for (Object item : c) {
                    if (item != null) {
                        ret.add(item);
                    } else if (allowNull) {
                        ret.add(null);  // BETWEEN、NOT BETWEEN 保留 null 值
                    }
                }
                return ret;
            }

        } else if (value.getClass().isArray()) {
            int size = Array.getLength(value);
            if (size > 0) {
                List<Object> ret = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    Object item = Array.get(value, i);
                    if (item != null) {
                        ret.add(item);
                    } else if (allowNull) {
                        ret.add(null);  // BETWEEN、NOT BETWEEN 保留 null 值
                    }
                }
                return ret;
            }

        } else if (allowSingleValue) {
            // IN、NOT IN 支持单个参数值，支持非集合类型参数
            List<Object> ret = new ArrayList<>(1);
            ret.add(value);
            return ret;

        } else {
            // BETWEEN、NOT BETWEEN 需要两个参数值，要求参数必须为数组或者 Collection
            throw new TemplateException(operator.sql() + " requires an array or Collection, but got: " + value.getClass().getName(), location);
        }

        return Collections.emptyList();
    }

    void generateInOrNotIn(Writer writer, boolean[] firstCondition, SqlPara sqlPara, List<?> list) {
        if (!list.isEmpty()) {
            generateConditionHead(writer, firstCondition);
            write(writer, "(");
            boolean first = true;
            for (Object item : list) {
                if (first) {
                    first = false;
                    write(writer, "?");
                } else {
                    write(writer, ", ?");
                }
                sqlPara.addPara(item);
            }
            write(writer, ")");
        }
    }

    // BETWEEN、NOT BETWEEN 要求参数为长度等于 2 的数组或 Collection，且两个元素都不为 null 时才生成 SQL
    void generateBetweenOrNotBetween(Writer writer, boolean[] firstCondition, SqlPara sqlPara, List<?> list) {
        if (list.size() != 2) {
            throw new TemplateException(operator.sql() + " requires exactly 2 arguments, but got: " + list.size(), location);
        }

        Object start = list.get(0);
        Object end = list.get(1);
        if (start != null && end != null) {
            generateConditionHead(writer, firstCondition);
            write(writer, " ? AND ?");
            sqlPara.addPara(start);
            sqlPara.addPara(end);
        }
    }
}



