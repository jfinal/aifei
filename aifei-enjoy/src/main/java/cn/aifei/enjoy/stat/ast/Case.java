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
import cn.aifei.enjoy.TemplateException;
import cn.aifei.enjoy.expr.ast.Expr;
import cn.aifei.enjoy.expr.ast.ExprList;
import cn.aifei.enjoy.io.Writer;
import cn.aifei.enjoy.stat.Location;
import cn.aifei.enjoy.stat.ParseException;
import cn.aifei.enjoy.stat.Scope;

/**
 * Case
 */
public class Case extends Stat implements CaseSetter {

    private Expr[] exprArray;
    private Stat stat;
    private Case nextCase;

    public Case(ExprList exprList, StatList statList, Location location) {
        if (exprList.length() == 0) {
            throw new ParseException("The parameter of #case directive can not be blank", location);
        }

        this.exprArray = exprList.getExprArray();
        this.stat = statList.getActualStat();
    }

    public void setNextCase(Case nextCase) {
        this.nextCase = nextCase;
    }

    public void exec(Env env, Scope scope, Writer writer) {
        throw new TemplateException("#case 指令的 exec 不能被调用", location);
    }

    boolean execIfMatch(Object switchValue, Env env, Scope scope, Writer writer) {
        if (exprArray.length == 1) {
            Object value = exprArray[0].eval(scope);

            // 照顾 null == null 以及数值比较小的整型数据比较
            if (value == switchValue) {
                stat.exec(env, scope, writer);
                return true;
            }

            if (value != null && value.equals(switchValue)) {
                stat.exec(env, scope, writer);
                return true;
            }
        } else {
            for (Expr expr : exprArray) {
                Object value = expr.eval(scope);

                // 照顾 null == null 以及数值比较小的整型数据比较
                if (value == switchValue) {
                    stat.exec(env, scope, writer);
                    return true;
                }

                if (value != null && value.equals(switchValue)) {
                    stat.exec(env, scope, writer);
                    return true;
                }
            }
        }

        return nextCase != null ? nextCase.execIfMatch(switchValue, env, scope, writer) : false;
    }
}


