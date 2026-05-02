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

package cn.aifei.router;

import java.lang.reflect.Method;
import cn.aifei.aop.Interceptor;
import cn.aifei.core.Input;
import cn.aifei.core.Output;
import cn.aifei.argument.Argument;

/**
 * Action
 */
public class Action {

    private final String actionPath;
    private final String targetPath;
    private final Interceptor[] interceptors;

    private final Class<?> targetClass;
    private final Method method;
    private final Argument<?, ?, ?>[] arguments;

    private final int matchParaCount;
    private final int pathParaCount;
    private final int methodParaCount;

    private String briefInfo;

    public Action(String actionPath, String targetPath, Interceptor[] interceptors, Class<?> targetClass, Method method, Argument<?, ?, ?>[] arguments) {
        this.actionPath = actionPath;
        this.targetPath = targetPath;
        this.interceptors = interceptors;

        this.targetClass = targetClass;
        this.method = method;
        this.method.setAccessible(true);
        this.arguments = arguments;

        int matchParaCount = 0;
        int pathParaCount = 0;
        int methodParaCount = 0;
        for (Argument<?, ?, ?> argument : arguments) {
            if (argument.isMatch()) {
                matchParaCount++;
                if (argument.isPathPara()) {
                    pathParaCount++;
                } else {
                    methodParaCount++;
                }
            }
        }
        this.matchParaCount = matchParaCount;
        this.pathParaCount = pathParaCount;
        this.methodParaCount = methodParaCount;
    }

    /**
     * 获取 actionPath，指向被调用的 action，包含 targetPath。
     */
    public String getActionPath() {
        return actionPath;
    }

    /**
     * 获取 targetPath，由 Path 注解或者 Routes.add(path, ...) 的 path 参数指定。
     */
    public String getTargetPath() {
        return targetPath;
    }

    /**
     * 获取作用于 action 的所有拦截器，按调用次序依次为：Routes 级、全局、Class 级、Method 级。
     */
    public Interceptor[] getInterceptors() {
        return interceptors;
    }

    /**
     * 获取 targetClass，由 Path 注解或者 Routes.add(..., target) 的 target 参数指定。
     */
    public Class<?> getTargetClass() {
        return targetClass;
    }

    /**
     * 获取 action 对应的 Method。
     */
    public Method getMethod() {
        return method;
    }

    /**
     * 获取 action 返回值类型。
     */
    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    /**
     * 获取 action 方法参数所关联的 Argument 对象，而后用于获取实参值。
     */
    @SuppressWarnings("unchecked")
    public <I extends Input, O extends Output> Argument<I, O, ?>[] getArguments() {
        return (Argument<I, O, ?>[]) arguments;
    }

    /**
     * 获取参与路由匹配的参数数量。
     */
    public int getMatchParaCount() {
        return matchParaCount;
    }

    /**
     * 获取路径参数数量，路径参数的 match 值必为 true。
     */
    public int getPathParaCount() {
        return pathParaCount;
    }

    /**
     * 获取方法参数数量，match 为 false 的不计。
     */
    public int getMethodParaCount() {
        return methodParaCount;
    }

    /**
     * 缓存 briefInfo，供开发模式下控制台输出 action info 时获取
     */
    public String getBriefInfo() {
        if (briefInfo == null) {
            briefInfo = buildActionBriefInfo();
        }
        return briefInfo;
    }

    private String buildActionBriefInfo() {
        StringBuilder ret = new StringBuilder(128)
                .append(targetClass.getName()).append('.')
                .append(method.getName()).append('(');
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                ret.append(", ");
            }
            ret.append(arguments[i].getType().getSimpleName()).append(' ').append(arguments[i].getName());
        }
        return ret.append(')').toString();
    }

    public String toString() {
        return actionPath + " -> " + getBriefInfo();
    }
}


