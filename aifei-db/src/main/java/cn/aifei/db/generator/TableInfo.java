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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TableInfo
 */
public class TableInfo implements Serializable {
	public String name;					// 表名
	public String[] primaryKey;			// 主键
	public String remarks;				// 表备注
	public boolean isView;				// 是否为 view

	public List<FieldInfo> fieldInfoList = new ArrayList<>();	// 字段信息 FieldInfo

	public TableInfo(String name, String[] primaryKey, String remarks, boolean isView) {
		Objects.requireNonNull(primaryKey, "primaryKey can not be null.");
		this.name = name;
		this.primaryKey = primaryKey;
		this.remarks = remarks;
		this.isView = isView;
	}

	// -------------------------------------------------------------------------------------
	// 以下变量生成器运行时赋值 ----------------------------------------------------------------

	public String modelName;
	public String baseModelName;
	public String daoName;
}



