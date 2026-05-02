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

package cn.aifei.core;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Input
 */
public interface Input {

    /**
     * 是否拥有名为 name 的数据
     */
    boolean has(String name);

    /**
     * 路径参数是否拥有位置为 index 的数据
     */
    boolean has(int index);

    /**
     * 设置 path parameter
     */
    void pathPara(String pathPara);

    // -------------------------------------------------------------------------------

    /**
     * 获取 Bean，与 getList 完备支持 json 顶层转换
     */
    <T> T getBean(String name, Class<T> type);

    /**
     * 获取 List，与 getBean 完备支持 json 顶层转换
     */
    <T> List<T> getList(String name, Class<T> type);

    /**
     * 获取数组
     */
    <T> T[] getArray(String name, Class<T> type);

    /**
     * 获取 Map
     */
    Map<String, Object> getMap(String name);

    // -----------------------------------------------------------------------------

    String getStr(String name);

    String getStr(int index);

    Integer getInt(String name);

    Integer getInt(int index);

    Long getLong(String name);

    Long getLong(int index);

    Double getDouble(String name);  // 不提供 getFloat，float 运算反而可能更慢，因为需要截断

    Double getDouble(int index);

    BigDecimal getBigDecimal(String name);

    BigDecimal getBigDecimal(int index);

    Boolean getBoolean(String name);

    Boolean getBoolean(int index);

    // -----------------------------------------------------------------------------

    Date getDate(String name);

    LocalDate getLocalDate(String name);

    LocalTime getLocalTime(String name);

    LocalDateTime getLocalDateTime(String name);

    // -----------------------------------------------------------------------------

    default String getStr(String name, String defaultValue) {
        String ret = getStr(name);
        return ret != null ? ret : defaultValue;
    }

    default String getStr(int index, String defaultValue) {
        String ret = getStr(index);
        return ret != null ? ret : defaultValue;
    }

    default Integer getInt(String name, Integer defaultValue) {
        Integer ret = getInt(name);
        return ret != null ? ret : defaultValue;
    }

    default Integer getInt(int index, Integer defaultValue) {
        Integer ret = getInt(index);
        return ret != null ? ret : defaultValue;
    }

    default Long getLong(String name, Long defaultValue) {
        Long ret = getLong(name);
        return ret != null ? ret : defaultValue;
    }

    default Long getLong(int index, Long defaultValue) {
        Long ret = getLong(index);
        return ret != null ? ret : defaultValue;
    }

    default Double getDouble(String name, Double defaultValue) {
        Double ret = getDouble(name);
        return ret != null ? ret : defaultValue;
    }

    default Double getDouble(int index, Double defaultValue) {
        Double ret = getDouble(index);
        return ret != null ? ret : defaultValue;
    }

    default BigDecimal getBigDecimal(String name, BigDecimal defaultValue) {
        BigDecimal ret = getBigDecimal(name);
        return ret != null ? ret : defaultValue;
    }

    default BigDecimal getBigDecimal(int index, BigDecimal defaultValue) {
        BigDecimal ret = getBigDecimal(index);
        return ret != null ? ret : defaultValue;
    }

    default Boolean getBoolean(String name, Boolean defaultValue) {
        Boolean ret = getBoolean(name);
        return ret != null ? ret : defaultValue;
    }

    default Boolean getBoolean(int index, Boolean defaultValue) {
        Boolean ret = getBoolean(index);
        return ret != null ? ret : defaultValue;
    }

    default Date getDate(String name, Date defaultValue) {
        Date ret = getDate(name);
        return ret != null ? ret : defaultValue;
    }

    default LocalDate getLocalDate(String name, LocalDate defaultValue) {
        LocalDate ret = getLocalDate(name);
        return ret != null ? ret : defaultValue;
    }

    default LocalTime getLocalTime(String name, LocalTime defaultValue) {
        LocalTime ret = getLocalTime(name);
        return ret != null ? ret : defaultValue;
    }

    default LocalDateTime getLocalDateTime(String name, LocalDateTime defaultValue) {
        LocalDateTime ret = getLocalDateTime(name);
        return ret != null ? ret : defaultValue;
    }
}



// 获取 File。直接注入 File 无法控制文件写入目录与文件名，也无法实现仅读内容不落盘功能，删之。
// File getFile(String name);

// 已通过 EnumArgument 实现枚举注入，目前不提供 getEnum
// <T extends Enum<T>> T getEnum(String name, Class<T> type);
// <T extends Enum<T>> T getEnum(int index, Class<T> type);
// default <T extends Enum<T>> T getEnum(String name, Class<T> type, T defaultValue) {
//     T ret = getEnum(name, type);
//     return ret != null ? ret : defaultValue;
// }
// default <T extends Enum<T>> T getEnum(int index, Class<T> type, T defaultValue) {
//     T ret = getEnum(index, type);
//     return ret != null ? ret : defaultValue;
// }


