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
import cn.aifei.enjoy.expr.ast.ExprList;
import cn.aifei.enjoy.io.Writer;
import cn.aifei.enjoy.stat.Scope;

/**
 * #and 指令生成 and 查询条件。只有在参数值不为 null 时才生成 sql，消除大量 #if 指令判断代码。
 *
 * <pre>
 * 一、用法
 *  1: 格式 #and(field, operator, para)
 *
 *  2: field 为生成到 sql 中的字符串。支持变量标识符用法与字符串用法，两种用法完全等价
 *        标识符用法：#and(name, '=', para)    将生成  and name = ?
 *        字符串用法：#and('name', '=', para)  也将生成 and name = ?
 *
 *     由于 field 这部分会原样生成到 sql ，所以用法更灵活，例如：
 *        #and('id - 1', '>', 100) 将生成 and id - 1 > 100 这样合法的 sql
 *
 *  3: operator 为操作符。必须是字符串常量
 *        #and(id, '=', id)
 *
 *     针对 like、not like 添加的扩展操作符:
 *        contains    等价于 like '%' + value + '%'
 *        notContains 等价于 not like '%' + value + '%'
 *        startsWith  等价于 like value + '%'
 *        endsWith    等价于 like '%' + value
 *
 *     注意：所有操作符仅支持全部大写或全部小写字母（notContains、startsWith、endsWith 除外），且前后不能有空格。
 *
 *  4: para 为查询条件项的参数值，para 将生成对应的问号占位符，且参数值会添加到 SqlPara 供 JDBC 使用
 *
 *     para 对应 sql(String, Map) 或 sqlById(String, Map) 方法 Map 参数中传递的参数，例如：
 *        Kv filter = Kv.of("anyName", "James");
 *        User.sql("select * from user where ... #and(name, '=', anyName)", filter);
 *        以上 #and 指令中的 anyName 对应 filter 中的 anyName，将通过 filter.get("anyName") 取出值并用于 sql 查询
 *
 *
 * 二、例子：
 *  1: 搭配 #where 指令使用
 *        Kv filter = Kv.of("age", 18).set("weight", 50);
 *        String sql = "select * from girl #where(age, '>', age) #and(weight, '<', weight)";
 *        Girl.sql(sql, filter).find();
 *
 *  2: 搭配 where 字符串使用
 *        select * from girl where age > 18 #and(weight, '<', weight)
 * </pre>
 */
public class AndDirective extends Directive {

    Condition condition;

    public void setExprList(ExprList exprList) {
        condition = new Condition(exprList, "#and", location);
    }

    @Override
    public void exec(Env env, Scope scope, Writer writer) {
        SqlPara sqlPara = (SqlPara) scope.get(SqlKit.SQL_PARA_KEY);
        if (sqlPara == null) {
            throw new TemplateException("#and must be used with sql(String, Map) or sqlById(String, Map)", location);
        }

        boolean[] firstCondition = (boolean[]) scope.get(WhereDirective.FIRST_CONDITION_KEY);
        if (firstCondition == null) {
            // throw new TemplateException("#and 指令前方需要使用 #where 指令", location);
            firstCondition = new boolean[]{false};  // 支持前方使用 where 字符串而非 #where 指令
        }
        condition.generate(scope, writer, firstCondition, sqlPara);
    }
}




