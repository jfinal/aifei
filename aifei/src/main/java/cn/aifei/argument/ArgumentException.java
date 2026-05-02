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

package cn.aifei.argument;

/**
 * ArgumentException 用于包装 Argument.getValue(...) 抛出的异常，
 * 便于调用者辨别为 Argument 异常并适当处理。
 */
public class ArgumentException extends Exception {

    static final StackTraceElement[] EMPTY_STACK_TRACE_ELEMENT = new StackTraceElement[0];

    Argument<?, ?, ?> argument;
    Throwable cause;

    public ArgumentException(Argument<?, ?, ?> argument, Throwable cause) {
        this.argument = argument;
        this.cause = cause;
    }

    /**
     * 获取实参对象
     */
    public Argument<?, ?, ?> getArgument() {
        return argument;
    }

    /**
     * 获取用空格分隔的实参类型和名称，便于向客户端输出异常对应的参数信息
     */
    public String getArgumentInfo() {
        return argument.getType().getSimpleName() + " " + argument.getName();
    }

    /**
     * 获取用空格分隔的实参类型和名称，再追加异常的 message
     */
    public String getArgumentInfoAndMessage() {
        return getArgumentInfo() + " -> " + cause.getMessage();
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    @Override
    public String getMessage() {
        return cause.getMessage();
    }

    @Override
    public String getLocalizedMessage() {
        return cause.getLocalizedMessage();
    }

    // 避免调用 Throwable.fillInStackTrace()
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    // 避免意外构建 stack trace
    @Override
    public StackTraceElement[] getStackTrace() {
        return EMPTY_STACK_TRACE_ELEMENT;
    }
}


