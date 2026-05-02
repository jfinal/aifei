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
import cn.aifei.enjoy.expr.ast.*;
import cn.aifei.enjoy.io.Writer;
import cn.aifei.enjoy.stat.Scope;

/**
 * #where 指令生成 where 查询条件。只有在参数值不为 null 时才生成 sql，消除大量 #if 指令判断代码。
 * 指令内部参数用法与 #and 指令完全一样，请移步 AndDirective 源码查看使用文档。
 *
 * <pre>
 * 例子：
 *   Kv filter = Kv.of("age", 18);
 *   String sql = "select * from girl #where(age, '>', age)";
 *   Girl.sql(sql, filter).find();
 * </pre>
 */
public class WhereDirective extends Directive {

    public static final String FIRST_CONDITION_KEY = "_FIRST_CONDITION_";

    Condition condition;

    public void setExprList(ExprList exprList) {
        condition = new Condition(exprList, "#where", location);
    }

    @Override
    public void exec(Env env, Scope scope, Writer writer) {
        SqlPara sqlPara = (SqlPara) scope.get(SqlKit.SQL_PARA_KEY);
        if (sqlPara == null) {
            throw new TemplateException("#where must be used with sql(String, Map) or sqlById(String, Map)", location);
        }

        boolean[] firstCondition = {true};                  // 是否为第一个生成的 condition
        scope.set(FIRST_CONDITION_KEY, firstCondition);     // 传递给后续 #and(...) 指令使用
        condition.generate(scope, writer, firstCondition, sqlPara);
    }
}



