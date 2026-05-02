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

package cn.aifei.aop;

import java.lang.reflect.Method;
import cn.aifei.argument.Argument;
import cn.aifei.argument.ArgumentException;
import cn.aifei.core.Input;
import cn.aifei.core.Output;
import cn.aifei.proxy.Callback;
import cn.aifei.router.Action;

/**
 * Invocation 调用 action method 及其之上配置的拦截器。
 */
public class Invocation {

    static final Object[] EMPTY_ARG_ARRAY = new Object[0];

    private Action action;
    private Object target;
    private Method method;
    private Callback callback;
    private Object[] args;
    private Object returnValue;

    private Input input;
    private Output output;

    private Interceptor[] interceptors;
    private int index = 0;

    public Invocation(Action action, Object target, Input input, Output output) {
        this.action = action;
        this.target = target;
        this.method = action.getMethod();
        this.interceptors = action.getInterceptors();

        this.input = input;
        this.output = output;
    }

    /**
     * 用于扩展 ProxyFactory
     */
    public Invocation(Object target, Method method, Object[] args, Interceptor[] interceptors, Callback callback) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.interceptors = interceptors;
        this.callback = callback;
    }

    /**
     * 调用 action method。调用 action method 之前先调用拦截器。
     */
    public void invoke() throws Throwable {
        if (index < interceptors.length) {
            interceptors[index++].intercept(this);

        } else if (index++ == interceptors.length) {    // index++ ensure invoke action only one time
            if (action != null) {
                // 拦截器调用之后获取 args，便于拦截器中处理并存入 Input 的数据能在 Argument.getValue(in, out) 中被使用
                Argument<Input, Output, ?>[] arguments = action.getArguments();
                if (arguments.length == 0) {
                    args = EMPTY_ARG_ARRAY;
                } else {
                    args = new Object[arguments.length];
                    int i = 0;
                    try {
                        for (; i < arguments.length; i++) {
                            args[i] = arguments[i].getValue(input, output);
                        }
                    } catch (Exception e) {
                        // invoke() 调用者可通过该异常辨别为实参异常并做出针对性处理，例如向客户端输出特定消息
                        throw new ArgumentException(arguments[i], e);
                    }
                }
                // Invoke the action
                returnValue = method.invoke(target, args);

            } else {
                // Invoke the callback
                returnValue = callback.call(args);
            }
        }
    }

    /**
     * 获取 actionPath。
     * actionPath = targetPath + methodName
     */
    public String getActionPath() {
        return action != null ? action.getActionPath() : null;
    }

    /**
     * 获取 targetPath
     */
    public String getTargetPath() {
        return action != null ? action.getTargetPath() : null;
    }

    /**
     * 获取当前 action 所在的目标对象
     */
    public Object getTarget() {
        return target;
    }

    /**
     * 获取当前 action 所对应的 Method 对象。可进一步调用 Method.getAnnotation()
     * 获取目标方法注解，以及调用 Method.getReturnType() 获取目标方法返回值。
     */
    public Method getMethod() {
        return method;
    }

    /**
     * 获取当前 action method 的方法名
     */
    public String getMethodName() {
        return method.getName();
    }

    /**
     * 获取当前 action method 所有实参值。用于在 IoHandler 中获取被注入的 Out 对象。
     */
    public Object[] getArgs() {
        return args;
    }

    /**
     * 获取当前 action method 下标为 index 的实参的值
     */
    public Object getArg(int index) {
        if (index >= args.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return args[index];
    }

    // args 改为在 invoke() 中获取后，setArg(...) 无法再使用
    // /**
    //  * 设置当前 action method 下标为 index 的实参的值
    //  */
    // public void setArg(int index, Object value) {
    //     if (index >= args.length) {
    //         throw new ArrayIndexOutOfBoundsException();
    //     }
    //     args[index] = value;
    // }

    /**
     * 获取当前 action method 调用之后的返回值
     */
    public Object getReturnValue() {
        return returnValue;
    }

    /**
     * 设置返回值给当前 action method
     */
    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    /**
     * 获取 Input
     */
    public Input getInput() {
        return input;
    }

    /**
     * 获取 Output
     */
    public Output getOutput() {
        return output;
    }
}


