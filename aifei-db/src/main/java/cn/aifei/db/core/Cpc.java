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

package cn.aifei.db.core;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 跨包调用 Cpc（Cross package call）。
 *
 * <pre>
 * Cpc 的目的：
 * 1: 隐藏使用频率低的方法。避免出现在 IDE 代码中提示中，减少认知负荷，提升开发体验。
 *
 * 2: 隐藏容易误用或造成破坏的方法。例如 AifeiRow.change() 使用不当可破坏 update() 行为。
 *
 * 3: 隐藏方法名不规范、不完美的方法，不规范是因为其它因素需要取舍。例如 AifeiRow.setOrPut(...)，
 *    添加下划线前缀是为避免 json 等框架将方法认定其为 setter，消耗一定性能甚至引起错误，
 *    即便将方法改为 protected 也不一定能避免消耗。
 * </pre>
 */
public class Cpc {

    /**
     * 转调 self.setOrPut(data)。
     *
     * <pre>
     * 典型应用场景 action 参数注入：
     * 1: json 数据式请求参数先通过 fastjson 转换为 Map data。
     * 2: 然后调用 Cpc.setOrPut(self, data) 将其整体注入继承自 AifeiRow 的 Model 对象。
     * </pre>
     */
    public static void setOrPut(AifeiRow<?> self, Map<String, Object> data) {
        self.setOrPut(data);
    }

    /**
     * 转调 self.setOrPut(dataRow)
     */
    public static void setOrPut(AifeiRow<?> self, AifeiRow<?> dataRow) {
        self.setOrPut(dataRow);
    }

    /**
     * 转调 self.change()，获取 AifeiRow 的 change set
     */
    public static Set<String> getChange(AifeiRow<?> self) {
        return self.change();
    }

    /**
     * 设置 AifeiRow 的 change set，允许为 null。
     */
    public static void setChange(AifeiRow<?> self, Set<String> change) {
        self.change = change;
    }

    /**
     * 独立配置 AifeiRow 的 TypeConverter
     *
     * <p>
     * 注意：在 DbConfig.setTypeConverter(...) 方法也会更新 AifeiRow.typeConverter 值
     */
    public static void setAifeiRowTypeConverter(TypeConverter typeConverter) {
        Objects.requireNonNull(typeConverter, "typeConverter can not be null.");
        AifeiRow.typeConverter = typeConverter;
    }
}



