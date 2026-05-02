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

import java.util.ArrayList;
import java.util.List;

/**
 * BatchResult 封装批量操作结果
 */
public class BatchResult {

    private long totalUpdateCounts = 0;
    private final List<Integer> updateCounts = new ArrayList<>();
    private final List<Object> generatedKeys = new ArrayList<>();

    public void addUpdateCounts(int[] updateCounts) {
        if (updateCounts != null) {
            for (Integer updateCount : updateCounts) {
                this.totalUpdateCounts += updateCount;
                this.updateCounts.add(updateCount);
            }
        }
    }

    public void addGeneratedKey(Object generatedKey) {
        if (generatedKey != null) {
            this.generatedKeys.add(generatedKey);
        }
    }

    /**
     * 获取插入与更新数据的总行数
     */
    public long getTotalUpdateCounts() {
        return totalUpdateCounts;
    }

    /**
     * 获取批量操作中每一个更新与插入影响的行数
     * 注意：如果数据按 table 与 fields 被分成了多个组，返回的 updateCounts 次序与原数据可能不一致。
     *       可通过配置 Batch.putUpdateCountsToRow(true) 将该值存入到对应的 Row 象之中再获取
     */
    public List<Integer> getUpdateCounts() {
        return updateCounts;
    }

    /**
     * 获取插入操作生成的主键值
     * 注意：如果数据按 table 与 fields 被分成了多个组，返回的主键值次序与原数据可能不一致。
     *      可从 Row 对象中获取生成的主键值
     */
    public List<Object> getGeneratedKeys() {
        return generatedKeys;
    }
}
