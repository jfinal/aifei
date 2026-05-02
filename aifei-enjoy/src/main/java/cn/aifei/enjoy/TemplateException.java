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

package cn.aifei.enjoy;

import cn.aifei.enjoy.stat.Location;

/**
 * Template runtime exception
 */
@SuppressWarnings("serial")
public class TemplateException extends RuntimeException {

    public TemplateException(String msg, Location loc) {
        super(loc != null ? msg + loc : msg);
    }

    public TemplateException(String msg, Location loc, Throwable cause) {
        super(loc != null ? msg + loc : msg, cause);
    }
}


