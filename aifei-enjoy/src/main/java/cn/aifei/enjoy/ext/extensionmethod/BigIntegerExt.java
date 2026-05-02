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
 * 针对 java.math.BigInteger 的扩展方法
 *
 * 用法：
 * #if(value.toInt() == 123)
 */
public class BigIntegerExt {
    public Boolean toBoolean(BigInteger self) {
        return !self.equals(BigInteger.ZERO);
    }

    public Integer toInt(BigInteger self) {
        return self.intValue();
    }

    public Long toLong(BigInteger self) {
        return self.longValue();
    }

    public Float toFloat(BigInteger self) {
        return self.floatValue();
    }

    public Double toDouble(BigInteger self) {
        return self.doubleValue();
    }

    public Short toShort(BigInteger self) {
        return self.shortValue();
    }

    public Byte toByte(BigInteger self) {
        return self.byteValue();
    }

    public BigInteger toBigInteger(BigInteger self) {
        return self;
    }

    public BigDecimal toBigDecimal(BigInteger self) {
        return new BigDecimal(self);
    }
}
