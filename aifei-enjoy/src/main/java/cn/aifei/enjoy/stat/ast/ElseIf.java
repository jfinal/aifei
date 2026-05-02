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
import cn.aifei.enjoy.expr.ast.Logic;
import cn.aifei.enjoy.io.Writer;
import cn.aifei.enjoy.stat.Location;
import cn.aifei.enjoy.stat.ParseException;
import cn.aifei.enjoy.stat.Scope;

/**
 * ElseIf
 */
public class ElseIf extends Stat {

    private Expr cond;
    private Stat stat;
    private Stat elseIfOrElse;

    public ElseIf(ExprList cond, StatList statList, Location location) {
        if (cond.length() == 0) {
            throw new ParseException("The condition expression of #else if statement can not be blank", location);
        }
        this.cond = cond.getActualExpr();
        this.stat = statList.getActualStat();
    }

    /**
     * take over setStat(...) method of super class
     */
    public void setStat(Stat elseIfOrElse) {
        this.elseIfOrElse = elseIfOrElse;
    }

    public void exec(Env env, Scope scope, Writer writer) {
        if (Logic.isTrue(cond.eval(scope))) {
            stat.exec(env, scope, writer);
        } else if (elseIfOrElse != null) {
            elseIfOrElse.exec(env, scope, writer);
        }
    }
}





