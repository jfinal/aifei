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

package cn.aifei.enjoy.proxy;

import java.util.Map;

/**
 * ProxyClass
 */
public class ProxyClass {

    // 被代理的目标
    private Class<?> target;

    /**
     * 以下是代理类信息
     */
    private String pkg;						// 包名
    private String name;						// 类名
    private String sourceCode;				// 源代码
    private Map<String, byte[]> byteCode;	// 字节码
    private Class<?> clazz;					// 字节码被 loadClass 后的 Class
    // private List<ProxyMethod> proxyMethodList = new ArrayList<>();

    public ProxyClass(Class<?> target) {
        this.target = target;
        this.pkg = target.getPackage().getName();
        this.name = target.getSimpleName() + "$$EnhancerByAifei";
    }

    /**
     * 是否需要代理
     */
    // public boolean needProxy() {
    // 	return proxyMethodList.size() > 0;
    // }

    public Class<?> getTarget() {
        return target;
    }

    public String getPkg() {
        return pkg;
    }

    public String getName() {
        return name;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public Map<String, byte[]> getByteCode() {
        return byteCode;
    }

    public void setByteCode(Map<String, byte[]> byteCode) {
        this.byteCode = byteCode;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    // public void addProxyMethod(ProxyMethod proxyMethod) {
    //     proxyMethodList.add(proxyMethod);
    // }

    // public List<ProxyMethod> getProxyMethodList() {
    //     return proxyMethodList;
    // }
}





