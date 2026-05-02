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

import java.nio.charset.Charset;
import cn.aifei.enjoy.EngineConfig;

/**
 * EncoderFactory
 */
public class EncoderFactory {

    protected Charset charset = Charset.forName(EngineConfig.DEFAULT_ENCODING);

    void setEncoding(String encoding) {
        charset = Charset.forName(encoding);
    }

    public Encoder getEncoder() {
        if (Charset.forName("UTF-8").equals(charset)) {
            return Utf8Encoder.me;
        } else {
            return new JdkEncoder(charset);
        }
    }
}




