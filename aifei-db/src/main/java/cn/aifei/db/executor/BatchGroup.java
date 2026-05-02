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

package cn.aifei.db.executor;

import cn.aifei.db.core.AifeiRow;
import java.util.ArrayList;
import java.util.List;

/**
 * BatchGroup 按 table、fields 对 Row 数据进行分组。
 * 具有相同 table + fields 值的 Row 数据才能在同一分组中共享一条 sql 进行批量操作
 */
public class BatchGroup<T extends AifeiRow<T>> {

    private final String tableAndFields;
    private final List<T> rowList = new ArrayList<>();

    public BatchGroup(String tableAndFields) {
        this.tableAndFields = tableAndFields;
    }

    public void add(T row) {
        rowList.add(row);
    }

    public String getTableAndFields() {
        return tableAndFields;
    }

    public List<T> getRowList() {
        return rowList;
    }
}

