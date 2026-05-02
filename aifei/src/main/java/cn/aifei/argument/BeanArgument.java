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

package cn.aifei.argument;

import cn.aifei.core.Input;
import cn.aifei.core.Output;

/**
 * BeanArgument 兜底
 */
public class BeanArgument<T> extends Argument<Input, Output, T> {

    @Override
    public T getValue(Input input, Output output) {
        // 只有一个路由匹配参数且 input 中不存在该参数，调用 getBean("", type) 免去使用 @Para(name = "")
        if (matchCount == 1 && !input.has(name)) {
            return input.getBean("", type);
        } else {
            return input.getBean(name, type);
        }
    }
}

