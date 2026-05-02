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

package cn.aifei.enjoy.io;

/**
 * IWritable 支持 OutputStream、Writer 双模式动态切换输出
 *
 * 详见 cn.aifei.enjoy.stat.ast.Text 中的用法
 */
public interface IWritable {

    /**
     * 供 OutputStream 模式下的 ByteWrite 使用
     */
    byte[] getBytes();

    /**
     * 供 Writer 模式下的 CharWrite 使用
     */
    char[] getChars();
}



