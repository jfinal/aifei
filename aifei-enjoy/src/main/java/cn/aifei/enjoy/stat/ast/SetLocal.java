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
 * SetLocal 设置局部变量
 *
 * 通常用于 #define #include 指令内部需要与外层作用域区分，以便于定义重用型模块的场景
 * 也常用于 #for 循环内部的临时变量
 */
public class SetLocal  extends Stat {

    final Expr expr;

    public SetLocal(ExprList exprList, Location location) {
        if (exprList.length() == 0) {
            throw new ParseException("The parameter of #setLocal directive can not be blank", location);
        }

        /* 放开对表达式类型的限定
        for (Expr expr : exprList.getExprArray()) {
            if ( !(expr instanceof Assign || expr instanceof IncDec) ) {
                throw new ParseException("#setLocal directive only supports assignment expressions", location);
            }
        }*/

        this.expr = exprList.getActualExpr();
    }

    public void exec(Env env, Scope scope, Writer writer) {
        Ctrl ctrl = scope.getCtrl();
        try {
            ctrl.setLocalAssignment();
            expr.eval(scope);
        } finally {
            ctrl.setWisdomAssignment();
        }
    }
}





