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
import cn.aifei.enjoy.stat.Ctrl;
import cn.aifei.enjoy.stat.Location;
import cn.aifei.enjoy.stat.ParseException;
import cn.aifei.enjoy.stat.Scope;

/**
 * SetGlobal 设置全局变量，全局作用域是指本次请求的整个 template
 * 
 * 适用于极少数的在内层作用域中希望直接操作顶层作用域的场景
 */
public class SetGlobal  extends Stat {

    private Expr expr;

    public SetGlobal(ExprList exprList, Location location) {
        if (exprList.length() == 0) {
            throw new ParseException("The parameter of #setGlobal directive can not be blank", location);
        }

        /* 放开对表达式类型的限定
        for (Expr expr : exprList.getExprArray()) {
            if ( !(expr instanceof Assign || expr instanceof IncDec) ) {
                throw new ParseException("#setGlobal directive only supports assignment expressions", location);
            }
        }*/

        this.expr = exprList.getActualExpr();
    }

    public void exec(Env env, Scope scope, Writer writer) {
        Ctrl ctrl = scope.getCtrl();
        try {
            ctrl.setGlobalAssignment();
            expr.eval(scope);
        } finally {
            ctrl.setWisdomAssignment();
        }
    }
}





