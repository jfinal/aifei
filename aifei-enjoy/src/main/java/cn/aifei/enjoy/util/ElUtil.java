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

package cn.aifei.enjoy.util;

import java.util.HashMap;
import java.util.Map;
import cn.aifei.enjoy.Directive;
import cn.aifei.enjoy.Engine;
import cn.aifei.enjoy.Env;
import cn.aifei.enjoy.Template;
import cn.aifei.enjoy.io.Writer;
import cn.aifei.enjoy.stat.Scope;

/**
 * EL 表达式语言求值工具类
 *
 * <pre>
 * 1：不带参示例
 * 	  Integer value = ElUtil.eval("1 + 2 * 3");
 *
 * 2：带参示例
 * 	  Map<String, Object> data = new HashMap<>();
 * 	  data.put("a", 2);
 * 	  data.put("b", 3);
 * 	  Integer value = ElUtil.eval("1 + a * b", data);
 * </pre>
 */
public class ElUtil {

    private static final Engine engine = new Engine();
    private static final String RETURN_VALUE_KEY = "_RETURN_VALUE_";

    static {
        engine.addDirective("eval", InnerEvalDirective.class, false);
    }

    public static Engine getEngine() {
        return engine;
    }

    public static <T> T eval(String expr) {
        return eval(expr, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    public static <T> T eval(String expr, Map<?, ?> data) {
        String stringTemplate = "#eval(" + expr + ")";
        Template template = engine.getTemplateByString(stringTemplate);
        template.render(data, (java.io.Writer)null);
        return (T)data.get(RETURN_VALUE_KEY);
    }

    public static class InnerEvalDirective extends Directive {
        public void exec(Env env, Scope scope, Writer writer) {
            Object value = exprList.eval(scope);
            scope.set(RETURN_VALUE_KEY, value);
        }
    }
}




