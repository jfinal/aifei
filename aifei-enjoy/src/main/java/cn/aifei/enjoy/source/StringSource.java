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

package cn.aifei.enjoy.source;

import cn.aifei.enjoy.util.StrUtil;
import cn.aifei.enjoy.EngineConfig;

/**
 * StringSource 用于从 String 变量中加载模板内容
 */
public class StringSource implements ISource {

    private String cacheKey;
    private StringBuilder content;

    /**
     * 构造 StringSource
     * @param content 模板内容
     * @param cache true 则缓存 Template，否则不缓存
     */
    public StringSource(String content, boolean cache) {
        this(content, cache ? content : null);
    }

    /**
     * 构造 StringSource
     * @param content 模板内容
     * @param cacheKey 缓存 Template 使用的 key，值为 null 时不缓存
     */
    public StringSource(String content, String cacheKey) {
        if (StrUtil.isBlank(content)) {
            throw new IllegalArgumentException("content can not be blank");
        }
        this.content = new StringBuilder(content);
        this.cacheKey = cacheKey;
    }

    public StringSource(StringBuilder content, boolean cache) {
        this(content, cache && content != null ? content.toString() : null);
    }

    public StringSource(StringBuilder content, String cacheKey) {
        if (content == null || content.length() == 0) {
            throw new IllegalArgumentException("content can not be blank");
        }
        this.content = content;
        this.cacheKey = cacheKey;			// cacheKey 值为 null 时不缓存
    }

    public boolean isModified() {
        return false;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public StringBuilder getContent() {
        return content;
    }

    public String getEncoding() {
        return EngineConfig.DEFAULT_ENCODING;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("cacheKey : ").append(cacheKey).append("\n");
        sb.append("content : ").append(content).append("\n");
        return sb.toString();
    }
}







