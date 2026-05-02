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

package cn.aifei.db.generator;

import cn.aifei.db.dialect.Dialect;
import cn.aifei.db.dialect.OracleDialect;
import cn.aifei.enjoy.util.StrUtil;

/**
 * fieldName 转化为 attrName
 */
@FunctionalInterface
public interface FieldToAttr {

    String convert(Dialect dialect, String fieldName);

    /**
     * MySql 数据库建议使用小写字段名或者驼峰字段名
     * Oracle 反射将得到大写字段名，所以不建议使用驼峰命名，建议使用下划线分隔单词命名法
     */
    class FieldToAttrImpl implements FieldToAttr {
        @Override
        public String convert(Dialect dialect, String fieldName) {
            if (dialect instanceof OracleDialect) {
                fieldName = fieldName.toLowerCase();
            }
            return StrUtil.toCamelCase(fieldName);
        }
    }
}
