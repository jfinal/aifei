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

import cn.aifei.enjoy.util.HashUtil;
import cn.aifei.enjoy.util.StrUtil;
import cn.aifei.enjoy.TemplateException;
import cn.aifei.enjoy.stat.Location;
import cn.aifei.enjoy.stat.ParseException;
import cn.aifei.enjoy.stat.Scope;

/**
 * Field
 * 
 * field 表达式取值优先次序，以 user.name 为例
 * 1：假如 user.getName() 存在，则优先调用
 * 2：假如 user 具有 public name 属性，则取 user.name 属性值
 * 3：假如 user 为 Model 子类，则调用 user.get("name")
 * 4：假如 user 为 Record，则调用 user.get("name")
 * 5：假如 user 为 Map，则调用 user.get("name")
 */
public class Field extends Expr {

    private Expr expr;
    private String fieldName;
    private String getterName;
    private long getterNameHash;

    // 可选链操作符 ?.
    private boolean optionalChain;

    public Field(Expr expr, String fieldName, boolean optionalChain, Location location) {
        if (expr == null) {
            throw new ParseException("The object for field access can not be null", location);
        }
        this.expr = expr;
        this.fieldName = fieldName;
        this.getterName = "get" + StrUtil.firstCharToUpperCase(fieldName);
        // fnv1a64 hash 到比 String.hashCode() 更大的 long 值范围
        this.getterNameHash = HashUtil.fnv1a64(getterName);
        this.optionalChain = optionalChain;
        this.location = location;
    }

    public Object eval(Scope scope) {
        Object target = expr.eval(scope);
        if (target == null) {
            if (optionalChain) {
                return null;
            }
            if (scope.getCtrl().isNullSafe()) {
                return null;
            }
            if (expr instanceof Id) {
                String id = ((Id)expr).getId();
                throw new TemplateException("\"" + id + "\" can not be null for accessed by \"" + id + "." + fieldName + "\"", location);
            }
            throw new TemplateException("Can not accessed by \"" + fieldName + "\" field from null target", location);
        }


        try {
            Class<?> targetClass = target.getClass();
            Object key = FieldKeyBuilder.instance.getFieldKey(targetClass, getterNameHash);
            FieldGetter fieldGetter = FieldKit.getFieldGetter(key, targetClass, fieldName);
            if (fieldGetter.notNull()) {
                return fieldGetter.get(target, fieldName);
            }
        } catch (TemplateException | ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new TemplateException(e.getMessage(), location, e);
        }


        if (scope.getCtrl().isNullSafe()) {
            return null;
        }
        if (expr instanceof Id) {
            String id = ((Id)expr).getId();
            throw new TemplateException("public field not found: \"" + id + "." + fieldName + "\" and public getter method not found: \"" + id + "." + getterName + "()\"", location);
        }
        throw new TemplateException("public field not found: \"" + fieldName + "\" and public getter method not found: \"" + getterName + "()\"", location);
    }

    // private Long buildFieldKey(Class<?> targetClass) {
        // return targetClass.getName().hashCode() ^ getterNameHash;
    // }
}






