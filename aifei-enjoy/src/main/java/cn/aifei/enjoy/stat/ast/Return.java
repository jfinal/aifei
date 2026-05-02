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
import cn.aifei.enjoy.io.Writer;
import cn.aifei.enjoy.stat.Scope;

/**
 * Return
 * 通常用于 #define 指令内部，不支持返回值
 */
public class Return  extends Stat {

    public static final Return me = new Return();

    private Return() {
    }

    public void exec(Env env, Scope scope, Writer writer) {
        scope.getCtrl().setReturn();
    }
}







