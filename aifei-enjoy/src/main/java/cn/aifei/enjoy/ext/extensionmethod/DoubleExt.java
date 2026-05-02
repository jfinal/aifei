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

package cn.aifei.enjoy.ext.extensionmethod;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 针对 java.lang.Double 的扩展方法
 *
 * 用法：
 * #if(value.toInt() == 123)
 */
public class DoubleExt {

    public Boolean toBoolean(Double self) {
        return self != 0;
    }

    public Integer toInt(Double self) {
        return self.intValue();
    }

    public Long toLong(Double self) {
        return self.longValue();
    }

    public Float toFloat(Double self) {
        return self.floatValue();
    }

    public Double toDouble(Double self) {
        return self;
    }

    public Short toShort(Double self) {
        return self.shortValue();
    }

    public Byte toByte(Double self) {
        return self.byteValue();
    }

    public BigInteger toBigInteger(Double self) {
        return BigInteger.valueOf(self.longValue());
    }

    public BigDecimal toBigDecimal(Double self) {
        return new BigDecimal(self);
    }
}




