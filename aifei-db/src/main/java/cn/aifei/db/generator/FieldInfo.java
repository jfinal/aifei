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
import java.io.Serializable;

/**
 * FieldInfo 封装表字段
 */
public class FieldInfo implements Serializable  {

	public String name;					// 字段名
	public String javaType;				// field 的 java 类型
	public String attrName;				// 字段对应的属性名

	public String remarks;				// 备注
	public Boolean isAutoIncrement;		// 是否自增

	public FieldInfo(String name, String javaType, String attrName, String remarks, Boolean isAutoIncrement) {
		if (StrUtil.isBlank(name)) {
			throw new RuntimeException("name can not be blank.");
		}
		if (StrUtil.isBlank(javaType)) {
			throw new RuntimeException("javaType can not be blank.");
		}
		if (StrUtil.isBlank(attrName)) {
			throw new RuntimeException("attrName can not be blank.");
		}

		this.name = name.trim();
		this.javaType = javaType.trim();
		this.attrName = attrName.trim();
		this.remarks = remarks;
		this.isAutoIncrement = isAutoIncrement;
	}
}

