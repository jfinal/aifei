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

package cn.aifei.enjoy.expr.ast;

import cn.aifei.enjoy.TemplateException;
import cn.aifei.enjoy.expr.ast.SharedMethodKit.SharedMethodInfo;
import cn.aifei.enjoy.stat.Location;
import cn.aifei.enjoy.stat.ParseException;
import cn.aifei.enjoy.stat.Scope;

/**
 * SharedMethod
 * 
 * 用法：
 * engine.addSharedMethod(new StrUtil());
 * engine.addSharedStaticMethod(MyKit.class);
 * 
 * #if (notBlank(para))
 *     ....
 * #end
 * 
 * 上面代码中的 notBlank 方法来自 StrUtil
 */
public class SharedMethod extends Expr {

    private SharedMethodKit sharedMethodKit;
    private String methodName;
    private ExprList exprList;

    public SharedMethod(SharedMethodKit sharedMethodKit, String methodName, ExprList exprList, Location location) {
        if (MethodKit.isForbiddenMethod(methodName)) {
            throw new ParseException("Forbidden method: " + methodName, location);
        }
        this.sharedMethodKit = sharedMethodKit;
        this.methodName = methodName;
        this.exprList = exprList;
        this.location = location;
    }

    public Object eval(Scope scope) {
        Object[] argValues = exprList.evalExprList(scope);

        try {
            SharedMethodInfo sharedMethodInfo = sharedMethodKit.getSharedMethodInfo(methodName, argValues);
            if (sharedMethodInfo != null) {
                return sharedMethodInfo.invoke(argValues);
            } else {
                // ShareMethod 相当于是固定的静态的方法，不支持 null safe，null safe 只支持具有动态特征的用法
                throw new TemplateException(Method.buildMethodNotFoundSignature("Shared method not found: ", methodName, argValues), location);
            }

        } catch (TemplateException | ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new TemplateException(e.getMessage(), location, e);
        }
    }
}




