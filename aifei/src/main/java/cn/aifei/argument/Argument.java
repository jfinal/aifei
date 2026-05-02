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

import cn.aifei.core.Input;
import cn.aifei.core.Output;
import cn.aifei.core.Para;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Argument 代表 action 实参，用于实现实参值的自由、灵活注入，本质上是 Method 级依赖注入，
 * 但与传统注入不同的是注入时可利用当前请求的上下文 Input、Output，便于将数据融入流程。
 *
 * <pre>
 * Argument 本质是依赖注入，常规实参注入只是最朴素用法。若当成依赖注入使用，拥有无限想象空间，例如：
 *  1: 根据用户登录会话信息，注入当前登录用户对象，无缝对接业务层，地表最爽登录会话实现。
 *  2: 注入 Sse，实现大模型流式交互。
 *  3: 与对三方对接时将 json 回转化为 Bean 交给业务层处理。
 *  4: 注入 Out 实现 excel 导出与文件下载。
 *  5: 注入可以持有 lambda 的对象，将用户逻辑先放在 lambda，然后在 Handler 中延迟处理，
 *     从而彻底消除业务层对 HttpServletRequest、HttpServletResponse、HttpServerExchange
 *     等等这类与业务无关类型的依赖，让业务变得纯粹。
 *     例如：Out.sendFile(...) 持有发送文件的 lambda 消除掉了对 HttpServerExchange 的依赖
 * </pre>
 */
public abstract class Argument<I extends Input, O extends Output, T> {

    /*
     * Argument 所属 action 参与路由匹配的参数数量（含 path para）：
     * 1: 去除了实现 NoMatch 接口的参数。
     * 2: 去除了实现 NoMatch 接口的 Argument 子类所服务的参数。
     * 3: 去除了使用 @Para(match = false) 注解的参数。
     * 4: 同属于一个 action 方法的所有 Argument 对象的 matchCount 值都相同。
     *
     * 简言之 matchCount 为参与路由匹配的参数数量。
     */
    protected int matchCount;

    protected Parameter parameter;
    protected String name;                  // 参数名
    protected Class<T> type;                // 参数类型
    protected Class<?> parameterizedType;   // 参数化类型，例如 List<User> 中的 User
    protected T defaultValue;               // 默认值

    protected boolean pathPara;             // 是否为路径参数 path parameter
    protected int index;                    // 路径参数位置
    protected boolean match;                // 多 action 具有相同 path 值，是否参与进一步的参数匹配

    @SuppressWarnings("unchecked")
    public void init(Parameter parameter) {
        Para paraAnn = parameter.getAnnotation(Para.class);

        this.parameter = parameter;
        this.name = initName(parameter, paraAnn);
        this.type = (Class<T>) parameter.getType();
        this.parameterizedType = initParameterizedType(parameter);
        this.defaultValue = initDefaultValue(paraAnn);

        this.pathPara = paraAnn != null && paraAnn.path();
        this.index = -1;    // 参数位置默认为 -1，后续会根据需要覆盖该值
        this.match = initMatch(paraAnn);
    }

    private boolean initMatch(Para paraAnn) {
        // 优先使用 Para 注解中的 match = false 值。如果为 true 则再判断是否实现了 NoMatch 接口
        if (paraAnn != null) {
            // match = true 为注解默认值，无法分辨是否为手工配置，暂不使用，后续探索更好设计
            // return paraAnn.match();

            // match = false 一定是手工配置，返回 false
            if (!paraAnn.match()) {
                if (pathPara) {
                    throw new IllegalArgumentException(
                            "Path parameters must participate in route matching; 'match' cannot be false: "
                                    + "@Para(path = true, match = false) " + type.getSimpleName() + " " + name
                    );
                }
                return false;
            }
        }

        // Argument 子类及其对应服务对象，只要其中之一实现了 NoMatch，都将置为 false
        if (this instanceof NoMatch || NoMatch.class.isAssignableFrom(type)) {
            if (pathPara) {
                throw new IllegalArgumentException(
                        "Implementations of the NoMatch interface cannot be configured as path parameters: "
                                + "@Para(path = true) " + type.getSimpleName() + " " + name + ". "
                                + "NoMatch implementation -> " + (NoMatch.class.isAssignableFrom(type) ? type.getName() : getClass().getName())
                );
            }
            return false;
        }

        // 默认参与路由匹配（action 方法存在重载才需匹配）
        return true;
    }

    /**
     * 获取参数名。name 允许为空字符串，且需要 trim()
     */
    private String initName(Parameter parameter, Para paraAnn) {
        if (paraAnn != null) {
            // 暂不支持 value()，可能被误认为是 Para value
            // if (!paraAnn.value().equals(Para.UNSET)) {
            //     return paraAnn.value().trim();
            // }
            if (!paraAnn.name().equals(Para.UNSET)) {
                return paraAnn.name().trim();
            }
        }
        return parameter.getName();
    }

    /**
     * 获取参数化类型，例如获取 List<User> 类型中的 User
     */
    private Class<?> initParameterizedType(Parameter parameter) {
        Type type = parameter.getParameterizedType();
        if (type instanceof ParameterizedType) {
            Type[] ts = ((ParameterizedType) type).getActualTypeArguments();
            if (ts != null && ts.length > 0) {
                return ts[0] instanceof Class ? (Class<?>) ts[0] : null;
            }
        }
        return null;
    }

    private T initDefaultValue(Para paraAnn) {
        if (paraAnn == null || paraAnn.defaultValue().equals(Para.UNSET)) {
            return null;
        }

        // return parseDefaultValue(paraAnn.defaultValue());
        String ret = paraAnn.defaultValue();
        if (type == String.class) {
            return parseDefaultValue(ret);   // String 默认值可包含空白字符，不能 trim()
        } else {
            ret = ret.trim();
            return ret.isEmpty() ? null : parseDefaultValue(ret);
        }
    }

    /**
     * 初始化 pathPara 参数的位置，非 pathPara 参数设置为 -1
     */
    public void initIndex(int index) {
        this.index = index;
    }

    /**
     * 初始化参与路由匹配的参数数量
     */
    public void initMatchCount(int matchCount) {
        this.matchCount = matchCount;
    }

    /**
     * 获取参数名
     */
    public String getName() {
        return name;
    }

    /**
     * 获取参数类型
     */
    public Class<T> getType() {
        return type;
    }

    /**
     * 是否为路由参数 path para
     */
    public boolean isPathPara() {
        return pathPara;
    }

    /**
     * 获取 pathPara 参数的位置，非 pathPara 参数值为 -1 不使用
     */
    public int getIndex() {
        return index;
    }

    /**
     * 是否参与路由匹配
     */
    public boolean isMatch() {
        return match;
    }

    /**
     * 获取参与路由匹配的参数数量
     */
    public int getMatchCount() {
        return matchCount;
    }

    /**
     * 将 @Para(defaultValue = ...) 注解配置的字符串转换为目标类型 T 作为默认值。
     * 注意：如果子类未重写该方法，那么对 defaultValue 进行配置会抛出 UnsupportedOperationException。
     */
    protected T parseDefaultValue(String str) {
        if (str == null) {
            return null;
        } else {
            throw new UnsupportedOperationException(getClass().getName()
                    + " must override Argument.parseDefaultValue(String) to handle @Para(defaultValue = \""
                    + str + "\") conversion."
            );
        }
    }

    /**
     * 获取参数值
     */
    public abstract T getValue(I input, O output);
}



