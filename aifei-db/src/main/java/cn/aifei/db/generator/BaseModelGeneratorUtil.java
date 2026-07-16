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

import cn.aifei.enjoy.util.StrUtil;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BaseModelGeneratorUtil
 * <p>
 * BaseModelGenerator 中某些在模板中书写不太方便的逻辑，通过 enjoy shared method 方式
 * 转移到此工具类中，提升可读性、可维护性，enjoy 尽可能只关注数据展示与生成。
 * <p>
 * 例如：createTableConst(...) 代码转移至此比在 enjoy 模板中书写要方便得多
 */
public class BaseModelGeneratorUtil {

    /**
     * 针对 Model 中可以自动转换类型的 getter 方法，调用其具有确定类型返回值的 getter 方法
     * 享用自动类型转换的便利性，例如 getInt(String)、getStr(String)
     * 其它方法使用泛型返回值方法： get(String)
     */
    private final Map<String, String> getterTypeMap = new HashMap<String, String>() {{
        put("String", "getStr");
        put("Integer", "getInt");
        put("Long", "getLong");
        put("Double", "getDouble");
        put("Float", "getFloat");

        // 新增两种可自动转换类型的 getter 方法
        put("java.util.Date", "getDate");
        put("java.time.LocalDateTime", "getLocalDateTime");

        // 新增 TypeKit 转换类之后，支持了更多的类型
        put("Boolean", "getBoolean");
        put("java.math.BigDecimal", "getBigDecimal");
        put("java.math.BigInteger", "getBigInteger");
    }};

    public Map<String, Object> createTableConst(TableInfo tableInfo) {
        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("name", tableInfo.name);

        StringBuilder fields = new StringBuilder();
        for (int i = 0; i < tableInfo.fieldInfoList.size(); i++) {
            if (i > 0) {
                fields.append(",");
            }
            fields.append(tableInfo.fieldInfoList.get(i).name);
        }
        ret.put("fields", fields);

        StringBuilder primaryKey = new StringBuilder();
        for (int i = 0; i < tableInfo.primaryKey.length; i++) {
            if (i > 0) {
                primaryKey.append(",");
            }
            primaryKey.append('"').append(tableInfo.primaryKey[i]).append('"');
        }
        ret.put("primaryKey", primaryKey);
        return ret;
    }

    // 获取单一主键的 java type
    public String getPrimaryKeyJavaType(TableInfo tableInfo) {
        for (FieldInfo fieldInfo : tableInfo.fieldInfoList) {
            if (fieldInfo.name.equalsIgnoreCase(tableInfo.primaryKey[0])) {
                return fieldInfo.javaType;
            }
        }

        // 若为视图 view 且没有主键，则使用 Object 类型
        if (tableInfo.isView) {
            return "Object";
        }

        throw new RuntimeException("未找到主键类型");
    }

    /**
     * 模板中通过 #set(getter = createGetterItem(tableInfo, fieldInfo)) 来得到该 shardMethod 的返回值
     */
    public Map<String, Object> createGetterItem(TableInfo tableInfo, FieldInfo fieldInfo) {
        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("methodName", "get" + StrUtil.firstCharToUpperCase(fieldInfo.attrName));
        String temp = getterTypeMap.get(fieldInfo.javaType);
        ret.put("getXxx", StrUtil.notBlank(temp) ? temp : "get");
        return ret;
    }

    /**
     * 模板中通过 #set(setter = createSetterItem(tableInfo, fieldInfo)) 来得到该 shardMethod 的返回值
     */
    public Map<String, Object> createSetterItem(TableInfo tableInfo, FieldInfo fieldInfo) {
        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("methodName", "set" + StrUtil.firstCharToUpperCase(fieldInfo.attrName));
        ret.put("attrName", JavaKeyword.me.contains(fieldInfo.attrName) ? "_" + fieldInfo.attrName : fieldInfo.attrName);

        // 创建 ShortSetter 的方法名
        createShortSetterName(fieldInfo, ret);
        return ret;
    }

    /**
     * ShortSetter 方法名如果是 table、primaryKey、data 则添加下划线后缀，否则与 AifeiRow 相关方法名冲突
     */
    private void createShortSetterName(FieldInfo fieldInfo, Map<String, Object> ret) {
        String attrName = fieldInfo.attrName;
        if ("table".equals(attrName) || "primaryKey".equals(attrName) || "data".equals(attrName)) {
            ret.put("shortSetterName", attrName + "_");
        } else {
            ret.put("shortSetterName", attrName);
        }
    }
}
