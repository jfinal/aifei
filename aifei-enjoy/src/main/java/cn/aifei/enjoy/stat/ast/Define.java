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
import cn.aifei.enjoy.stat.Location;
import cn.aifei.enjoy.stat.ParseException;
import cn.aifei.enjoy.stat.Scope;
import cn.aifei.enjoy.expr.ast.Expr;
import cn.aifei.enjoy.expr.ast.ExprList;
import cn.aifei.enjoy.expr.ast.Id;
import cn.aifei.enjoy.io.Writer;

/**
 * Define 定义模板函数：
 * #define funcName(p1, p2, ..., pn)
 *   body
 * #end
 *
 * 模板函数类型：
 * 1：全局共享的模板函数
 *   通过 engine.addSharedFunction(...) 添加，所有模板中可调用
 * 2：模板中定义的局部模板函数
 *   在模板中定义的模板函数，只在本模板中有效
 *
 * 高级用法：
 * 1：局部模板函数可以与全局共享模板函数同名，调用时优先调用模板内模板数
 * 2：模板内部不能定义同名的局部模板函数
 */
public class Define extends Stat {

    private static final String[] NULL_PARAMETER_NAMES = new String[0];

    private String functionName;
    private String[] parameterNames;
    private Stat stat;

    public Define(String functionName, ExprList exprList, StatList statList, Location location) {
        setLocation(location);
        this.functionName = functionName;
        this.stat = statList.getActualStat();

        Expr[] exprArray = exprList.getExprArray();
        if (exprArray.length == 0) {
            this.parameterNames = NULL_PARAMETER_NAMES;
            return ;
        }

        parameterNames = new String[exprArray.length];
        for (int i=0; i<exprArray.length; i++) {
            if (exprArray[i] instanceof Id) {
                parameterNames[i] = ((Id)exprArray[i]).getId();
            } else {
                throw new ParseException("The parameter of template function definition must be identifier", location);
            }
        }
    }

    public String getFunctionName() {
        return functionName;
    }

    public String[] getParameterNames() {
        return parameterNames;
    }

    /**
     * Define 的继承类可以覆盖此方法实现一些 register 类的动作
     */
    public void exec(Env env, Scope scope, Writer writer) {

    }

    /**
     * 真正调用模板函数
     */
    public void call(Env env, Scope scope, ExprList exprList, Writer writer) {
        if (exprList.length() != parameterNames.length) {
            throw new TemplateException("Wrong number of argument to call the template function, right number is: " + parameterNames.length, location);
        }

        scope = new Scope(scope);
        if (exprList.length() > 0) {
            Object[] parameterValues = exprList.evalExprList(scope);
            for (int i=0; i<parameterValues.length; i++) {
                scope.setLocal(parameterNames[i], parameterValues[i]);	// 参数赋值
            }
        }

        stat.exec(env, scope, writer);
        scope.getCtrl().setJumpNone();	// #define 中的 return、continue、break 全部在此消化
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("#define ").append(functionName).append("(");
        for (int i=0; i<parameterNames.length; i++) {
            if (i > 0) {
                ret.append(", ");
            }
            ret.append(parameterNames[i]);
        }
        return ret.append(")").toString();
    }

    // -----------------------------------------------------------------------

    /**
     * envForDevMode 属性以及相关方法仅用于 devMode 判断当前 #define 指令所在资源是否被修改
     * 仅用于 EngineConfig 中处理 shared function 的逻辑
     */
    private Env envForDevMode;

    public void setEnvForDevMode(Env envForDevMode) {
        this.envForDevMode = envForDevMode;
    }

    public boolean isSourceModifiedForDevMode() {
        if (envForDevMode == null) {
            throw new IllegalStateException("Check engine config: setDevMode(...) must be invoked before addSharedFunction(...)");
        }
        return envForDevMode.isSourceListModified();
    }
}



