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

package cn.aifei.db.factory;

import cn.aifei.db.core.Batch;
import cn.aifei.db.core.DbConfig;
import java.io.Serializable;

/**
 * Batch 实例工厂。支持通过继承 Batch 并定制个性化 Batch 实现类，然后通过 BatchFactory 切换为自己的 Batch 实现。
 */
public class BatchFactory implements Serializable {
    public Batch get(DbConfig config) {
        return new Batch(config);
    }
}

