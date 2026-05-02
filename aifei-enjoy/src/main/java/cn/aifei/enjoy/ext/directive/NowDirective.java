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

import java.util.Date;
import cn.aifei.enjoy.Directive;
import cn.aifei.enjoy.Env;
import cn.aifei.enjoy.TemplateException;
import cn.aifei.enjoy.expr.ast.ExprList;
import cn.aifei.enjoy.io.Writer;
import cn.aifei.enjoy.stat.ParseException;
import cn.aifei.enjoy.stat.Scope;

/**
 * 根据 datePattern 参数输出当前时间，未指定 datePattern 参数时默认使用以下配置
 * env.getEngineConfig().getDatePattern()
 * 
 * 注意该指令需要配置才能使用：
 * engine.addDirective("now", NowDirective.class, true);
 * 
 * 例子：
 * 1：#now()
 * 2：#now("HH:mm:ss")
 * 3：#now("yyyy-MM-dd HH:mm:ss")
 */
public class NowDirective extends Directive {

    public void setExprList(ExprList exprList) {
        if (exprList.length() > 1) {
            throw new ParseException("#now directive support one parameter only", location);
        }
        super.setExprList(exprList);
    }

    public void exec(Env env, Scope scope, Writer writer) {
        String datePattern;
        if (exprList.length() == 0) {
            datePattern = env.getEngineConfig().getDatePattern();
        } else {
            Object dp = exprList.eval(scope);
            if (dp instanceof String) {
                datePattern = (String)dp;
            } else {
                throw new TemplateException("The parameter of #now directive must be String", location);
            }
        }

        try {
            writer.write(new Date(), datePattern);
        } catch (Exception e) {
            throw new TemplateException(e.getMessage(), location, e);
        }
    }
}



