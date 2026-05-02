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

import cn.aifei.enjoy.Directive;
import cn.aifei.enjoy.Env;
import cn.aifei.enjoy.Template;
import cn.aifei.enjoy.expr.ast.Const;
import cn.aifei.enjoy.expr.ast.Expr;
import cn.aifei.enjoy.expr.ast.ExprList;
import cn.aifei.enjoy.io.Writer;
import cn.aifei.enjoy.stat.ParseException;
import cn.aifei.enjoy.stat.Scope;
import java.util.Map;

/**
 * SqlDirective
 */
public class SqlDirective extends Directive {

    private String id;

    @Override
    public void setExprList(ExprList exprList) {
        if (exprList.length() == 0) {
            throw new ParseException("The parameter of #sql directive can not be blank", location);
        }
        if (exprList.length() > 1) {
            throw new ParseException("Only one parameter allowed for #sql directive", location);
        }
        Expr expr = exprList.getExpr(0);
        if (expr instanceof Const && ((Const) expr).isStr()) {
            this.id = ((Const) expr).getStr();
        } else {
            throw new ParseException("The parameter of #sql directive must be String", location);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void exec(Env env, Scope scope, Writer writer) {
        Map<String, Template> sqlCache = (Map<String, Template>) scope.get(SqlKit.SQL_CACHE_KEY);
        if (sqlCache.containsKey(id)) {
            throw new ParseException("Sql already exists with id : " + id, location);
        }

        sqlCache.put(id, new Template(env, stat));
    }

    @Override
    public boolean hasEnd() {
        return true;
    }
}

