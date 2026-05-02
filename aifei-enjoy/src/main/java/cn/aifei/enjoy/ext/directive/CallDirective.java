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

package cn.aifei.enjoy.ext.directive;

import java.util.ArrayList;
import cn.aifei.enjoy.Directive;
import cn.aifei.enjoy.Env;
import cn.aifei.enjoy.TemplateException;
import cn.aifei.enjoy.expr.ast.Const;
import cn.aifei.enjoy.expr.ast.Expr;
import cn.aifei.enjoy.expr.ast.ExprList;
import cn.aifei.enjoy.io.Writer;
import cn.aifei.enjoy.stat.ParseException;
import cn.aifei.enjoy.stat.Scope;
import cn.aifei.enjoy.stat.ast.Define;

/**
 * CallDirective 动态调用模板函数
 *
 * 模板函数的名称与参数都可以动态指定，提升模板函数调用的灵活性
 *
 * 例如：
 *     #call(funcName, p1, p2, ..., pn)
 *     其中 funcName，为函数名，p1、p2、pn 为被调用函数所使用的参数
 *
 *
 * 如果希望模板函数不存在时忽略其调用，添加常量值 true 在第一个参数位置即可
 * 例如：
 *     #call(true, funcName, p1, p2, ..., pn)
 *
 *
 * TODO 后续优化看一下 ast.Call.java
 */
public class CallDirective extends Directive {

    protected Expr funcNameExpr;
    protected ExprList paraExpr;

    protected boolean nullSafe = false;		// 是否支持函数名不存在时跳过

    public void setExprList(ExprList exprList) {
        int len = exprList.length();
        if (len == 0) {
            throw new ParseException("Template function name required", location);
        }

        int index = 0;
        Expr expr = exprList.getExpr(index);
        if (expr instanceof Const && ((Const)expr).isBoolean()) {
            if (len == 1) {
                throw new ParseException("Template function name required", location);
            }

            nullSafe = ((Const)expr).getBoolean();
            index++;
        }

        funcNameExpr = exprList.getExpr(index++);

        ArrayList<Expr> list = new ArrayList<>();
        for (int i=index; i<len; i++) {
            list.add(exprList.getExpr(i));
        }
        paraExpr = new ExprList(list);
    }

    public void exec(Env env, Scope scope, Writer writer) {
        Object funcNameValue = funcNameExpr.eval(scope);
        if (funcNameValue == null) {
            if (nullSafe) {
                return ;
            }
            throw new TemplateException("Template function name can not be null", location);
        }

        if (!(funcNameValue instanceof String)) {
            throw new TemplateException("Template function name must be String", location);
        }

        Define func = env.getFunction(funcNameValue.toString());

        if (func == null) {
            if (nullSafe) {
                return ;
            }
            throw new TemplateException("Template function not found : " + funcNameValue, location);
        }

        func.call(env, scope, paraExpr, writer);
    }
}




