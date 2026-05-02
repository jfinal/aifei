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

package cn.aifei.db.ext;

import cn.aifei.db.core.AifeiRow;
import cn.aifei.enjoy.expr.ast.FieldGetter;

/**
 * AifeiRowFieldGetter.
 *
 * <pre>
 * 配置方法：
 *     Engine.addFieldGetterToLast(new AifeiRowFieldGetter());
 * 注意：该配置全局有效
 *
 * 支持 Enjoy 属性访问表达式调用 AifeiRow.get(String) 方法获取数据，例如:
 *     #(user.name)
 * </pre>
 */
public class AifeiRowFieldGetter extends FieldGetter {

    // 所有 AifeiRow 共享 singleton 避免创建对象
    static final AifeiRowFieldGetter singleton = new AifeiRowFieldGetter();

    public FieldGetter takeOver(Class<?> targetClass, String fieldName) {
        if (AifeiRow.class.isAssignableFrom(targetClass)) {
            return singleton;   // 返回共享单例，避免创建对象
        } else {
            return null;
        }
    }

    public Object get(Object target, String fieldName) throws Exception {
        return ((AifeiRow<?>) target).get(fieldName);
    }
}


