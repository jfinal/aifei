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
import cn.aifei.enjoy.Directive;
import cn.aifei.enjoy.Env;
import cn.aifei.enjoy.TemplateException;
import cn.aifei.enjoy.expr.ast.Const;
import cn.aifei.enjoy.expr.ast.Expr;
import cn.aifei.enjoy.expr.ast.ExprList;
import cn.aifei.enjoy.expr.ast.Id;
import cn.aifei.enjoy.io.Writer;
import cn.aifei.enjoy.stat.ParseException;
import cn.aifei.enjoy.stat.Scope;
import java.lang.reflect.Array;
import java.util.*;

/**
 * #orderBy() 指令生成 order by 子句
 *
 * <pre>
 * 设计：
 *   1: 指令格式
 *         #orderBy(f1, f2, ..., fn)
 *      其中 f1、f2、..., fn 为可用于排序的字段白名单，用于防止 sql 注入
 *
 *   2: 前端后端约定默认参数名为 orderBy
 *
 *   3: 前端参数格式
 *       > 支持单字段排序，使用 json 对象
 *         orderBy: {field: 'updated', order: 'desc'}
 *
 *       > 支持多字段排序，使用 json 数组
 *         orderBy: [
 *             {field: 'updated', order: 'desc'},
 *             {field: 'age', order: 'asc'}
 *         ]
 *
 *   4: 后端 #order 指令使用白名单指定可排序的字段名，防止 sql 注入
 *         #orderBy(updated, age)
 *      以上参数名 updated、age 为可进行排序的白名单
 *
 *   5: 后端 sql 字段映射到前端字段。例：后端 updated_time 映射到前端 updateTime
 *         后端 #orderBy('updated_time:updateTime')
 *         前端 orderBy: { field: 'updateTime', order: 'DESC' }
 *      以上用法将后端 sql 字段 updated_time 映射到了前端字段 updateTime
 *
 *   6: 打破前后端约定的默认参数名 orderBy，添加一个带字符 '$' 前缀字符的变量指定即可，例如：
 *        前端 myOrder: { ... }
 *        后端 #orderBy($myOrder, ...)
 * </pre>
 */
public class OrderByDirective extends Directive {

    static final String DEFAULT_PARA_NAME = "orderBy";

    static final Map<String, String> ORDER_WHITELIST;

    static {
        ORDER_WHITELIST = new HashMap<>();
        ORDER_WHITELIST.put("asc", "ASC");
        ORDER_WHITELIST.put("ASC", "ASC");
        ORDER_WHITELIST.put("desc", "DESC");
        ORDER_WHITELIST.put("DESC", "DESC");
    }

    String paraName = DEFAULT_PARA_NAME;
    Map<String, String> fieldWhitelist = new LinkedHashMap<>();

    public void setExprList(ExprList exprList) {
        int len = exprList.length();
        if (len == 0) {
            throw new ParseException("#orderBy() requires at least 1 argument", location);
        }

        for (int i = 0; i < len; i++) {
            String str;
            Expr expr = exprList.getExpr(i);
            if (expr instanceof Id) {
                str = ((Id) expr).getId();
            } else if (expr instanceof Const && ((Const) expr).isStr()) {
                str = ((Const) expr).getStr().trim();
            } else {
                throw new ParseException("#orderBy() arguments must be identifiers or string literals", location);
            }

            if (str.startsWith("$")) {
                if (i != 0) {
                    throw new ParseException("#orderBy() parameter starting with '$' must be the first argument, but found at position: " + (i + 1), location);
                }
                paraName = str.substring(1);
                if (paraName.isEmpty()) {
                    throw new ParseException("#orderBy() parameter name after '$' must not be empty", location);
                }
                continue;
            }

            int index = str.indexOf(':');
            if (index == -1) {
                fieldWhitelist.put(str, str);
            } else {
                String sqlField = str.substring(0, index).trim();
                String clientField = str.substring(index + 1).trim();
                if (sqlField.isEmpty() || clientField.isEmpty()) {
                    throw new ParseException("#orderBy() invalid whitelist format, expected \"sqlField:clientField\": " + str, location);
                }
                fieldWhitelist.put(clientField, sqlField);
            }
        }

        if (fieldWhitelist.isEmpty()) {
            throw new ParseException("#orderBy() requires at least one sortable field in whitelist", location);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void exec(Env env, Scope scope, Writer writer) {
        SqlPara sqlPara = (SqlPara) scope.get(SqlKit.SQL_PARA_KEY);
        if (sqlPara == null) {
            throw new TemplateException("#orderBy() must be used with sql(String, Map) or sqlById(String, Map)", location);
        }

        Object orderBy = scope.get(paraName);
        // 排序参数未传递不生成 order by 语句
        if (orderBy == null) {
            return;
        }

        // 单字段排序
        if (orderBy instanceof Map) {
            Map<Object, Object> orderByMap = (Map<Object, Object>) orderBy;
            if (!orderByMap.isEmpty()) {
                generateOrderByItem(orderByMap, writer, true);
            }
            return;
        }

        // 多字段排序
        List<Map<Object, Object>> list = toOrderByItemList(orderBy);
        if (!list.isEmpty()) {
            boolean first = true;
            for (Map<Object, Object> map : list) {
                generateOrderByItem(map, writer, first);
                first = false;
            }
        }
    }

    @SuppressWarnings("unchecked")
    List<Map<Object, Object>> toOrderByItemList(Object orderBy) {
        if (orderBy instanceof Collection) {
            Collection<?> c = (Collection<?>) orderBy;
            if (!c.isEmpty()) {
                List<Map<Object, Object>> ret = new ArrayList<>(c.size());
                for (Object item : c) {
                    if (item != null) {
                        if (!(item instanceof Map)) {
                            throw new TemplateException("#orderBy() requires Map items, but got: " + item.getClass().getName(), location);
                        }
                        ret.add((Map<Object, Object>) item);
                    }
                }
                return ret;
            }

        } else if (orderBy.getClass().isArray()) {
            int size = Array.getLength(orderBy);
            if (size > 0) {
                List<Map<Object, Object>> ret = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    Object item = Array.get(orderBy, i);
                    if (item != null) {
                        if (!(item instanceof Map)) {
                            throw new TemplateException("#orderBy() requires Map items, but got: " + item.getClass().getName(), location);
                        }
                        ret.add((Map<Object, Object>) item);
                    }
                }
                return ret;
            }

        } else {
            throw new TemplateException("#orderBy() parameter must be Map, Collection, or array, but got: " + orderBy.getClass().getName(), location);
        }

        return Collections.emptyList();
    }

    void generateOrderByItem(Map<Object, Object> orderByItem, Writer writer, boolean first) {
        // 检测排序参数 field、order 是否已传递
        Object fieldFromClient = orderByItem.get("field");
        if (fieldFromClient == null) {
            throw new TemplateException("orderBy field must not be null", location);
        }
        Object orderFromClient = orderByItem.get("order");
        if (orderFromClient == null) {
            throw new TemplateException("orderBy order must not be null", location);
        }

        // 检测 field、order 是否处于白名单
        String field = fieldWhitelist.get(fieldFromClient.toString().trim());
        if (field == null) {
            throw new TemplateException("orderBy field not in whitelist: " + fieldFromClient, location);
        }
        String order = ORDER_WHITELIST.get(orderFromClient.toString().trim());
        if (order == null) {
            throw new TemplateException("orderBy order must be asc or desc, but got: " + orderFromClient, location);
        }

        // 生成 ORDER BY 语句
        write(writer, first ? "ORDER BY " : ", ");
        write(writer, field);
        write(writer, " ");
        write(writer, order);
    }
}





