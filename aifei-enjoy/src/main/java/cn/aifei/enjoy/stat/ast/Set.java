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

package cn.aifei.enjoy.stat.ast;

import cn.aifei.enjoy.Env;
import cn.aifei.enjoy.expr.ast.Expr;
import cn.aifei.enjoy.expr.ast.ExprList;
import cn.aifei.enjoy.io.Writer;
import cn.aifei.enjoy.stat.Location;
import cn.aifei.enjoy.stat.ParseException;
import cn.aifei.enjoy.stat.Scope;

/**
 * Set 赋值，从内向外作用域查找变量，找到则替换变量值，否则在顶层作用域赋值
 * 
 * 用法：
 * 1：#set(k = v)
 * 2：#set(k1 = v1, k2 = v2, ..., kn = vn)
 * 3：#set(x = 1+2)
 * 4：#set(x = 1+2, y = 3>4, ..., z = c ? a : b)
 */
public class Set extends Stat {

    private Expr expr;

    public Set(ExprList exprList, Location location) {
        if (exprList.length() == 0) {
            throw new ParseException("The parameter of #set directive can not be blank", location);
        }

        /* 放开对表达式类型的限定
        for (Expr expr : exprList.getExprArray()) {
            if ( !(expr instanceof Assign || expr instanceof IncDec) ) {
                throw new ParseException("#set directive only supports assignment expressions", location);
            }
        }*/

        this.expr = exprList.getActualExpr();
    }

    public void exec(Env env, Scope scope, Writer writer) {
        scope.getCtrl().setWisdomAssignment();
        expr.eval(scope);
    }
}

