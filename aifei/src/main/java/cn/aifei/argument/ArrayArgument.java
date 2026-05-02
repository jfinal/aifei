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
import java.lang.reflect.Array;
import java.lang.reflect.Parameter;

/**
 * ArrayArgument
 */
public class ArrayArgument<T> extends Argument<Input, Output, T[]> {

    private Class<T> componentType;

    @Override
    public void init(Parameter parameter) {
        this.componentType = getComponentType(parameter);   // 必须放 super.init() 之前
        super.init(parameter);
    }

    @SuppressWarnings("unchecked")
    private Class<T> getComponentType(Parameter parameter) {
        // Type t = parameter.getParameterizedType();
        // if (! (t instanceof GenericArrayType)) {
        //     throw new RuntimeException("泛型错误");
        // }
        // GenericArrayType gat = (GenericArrayType) t;
        // Type genericComponentType = gat.getGenericComponentType();
        // return (Class<T>) genericComponentType.getClass();

        return (Class<T>) parameter.getType().getComponentType();
    }

    /**
     * 目前仅支持 String[]、Integer[]、Long[] 三种数组的默认值配置：
     * 1: 配置为 "[]" 支持长度为 0 的空数组。
     * 2: 逗号分隔每个默认值。注意无需使用字符 '[' 与 ']'。
     */
    @Override @SuppressWarnings("unchecked")
    protected T[] parseDefaultValue(String str) {
        // 配置为 "[]" 支持长度为 0 的空数组
        if ("[]".equals(str)) {
            return (T[]) Array.newInstance(componentType, 0);
        }

        String[] strArray = str.split(",");
        T[] ret = (T[]) Array.newInstance(componentType, strArray.length);

        for (int i = 0; i < strArray.length; i++) {
            if (componentType == String.class) {
                ret[i] = (T) strArray[i].trim();
            } else if (componentType == Integer.class) {
                ret[i] = (T) Integer.valueOf(strArray[i].trim());
            } else if (componentType == Long.class) {
                ret[i] = (T) Long.valueOf(strArray[i].trim());
            } else {
                return null;    // 其它类型直接返回 null
            }
        }

        return ret;
    }

    @Override
    public T[] getValue(Input input, Output output) {
        T[] ret;

        // 只有一个路由匹配参数且 input 中不存在该参数，调用 getArray("", type) 免去使用 @Para(name = "")
        if (matchCount == 1 && !input.has(name)) {
            ret = input.getArray("", componentType);
        } else {
            ret = input.getArray(name, componentType);
        }

        return ret != null ? ret : defaultValue;
    }
}



