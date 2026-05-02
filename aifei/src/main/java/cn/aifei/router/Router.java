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

import cn.aifei.aop.Aop;
import cn.aifei.aop.AopKit;
import cn.aifei.aop.Interceptor;
import cn.aifei.aop.InterceptorKit;
import cn.aifei.core.*;
import cn.aifei.argument.Argument;
import cn.aifei.argument.ArgumentKit;
import cn.aifei.log.Log;
import cn.aifei.scanner.Scanner;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Router.
 *
 * <pre>
 * 路由扫描设计：
 *  1: 参数 String basePackage 为扫描基础包，scan 方法将扫描该包及其子包所有声明了 "类级别 @Path 注解" 的目标。
 *
 *  2: 参数 Interceptor[] interceptors 为 Routes 级别拦截器，当前扫描方法扫描到的目标类都将被配置上这些拦截器。
 *      Routes 级别拦截器调用时机早于全局拦截器，将最先被调用。
 *
 *  3: 参数 Predicate<Class<?>> skip 用于跳过当前扫描过程中不需要的目标类。
 *
 *  4: 支持调用 scan 方法多次扫描，如果同一目标被多次扫描到，以第一次为准。
 *</pre>
 *
 * <pre>
 * 路由匹配设计：
 *  1: path parameter 属于路由的一部分必须参与匹配。
 *
 *  2: 命名参数（类似 query/form/body 的语义）大量是可缺省的例如：筛选条件、分页字段、排序、可选开关，在单 action 时不参与路由匹配。
 *
 *  3: 在 action group 场景即多个 action 共享同一路由时，命名参数必须参与路由匹配，而且优先匹配参数数量多的 action，否则调用到错误的
 *     action 有损系统健壮性与安全性。
 *</pre>
 */
public class Router {

    static final String SLASH = "/";
    static final Interceptor[] EMPTY_INTERS = new Interceptor[0];
    static final Log log = Log.get(Router.class);

    final Set<Class<?>> allScannedSet = new LinkedHashSet<>();
    final Map<String, Object> mapping = new HashMap<>(2048, 2F / 3F);

    boolean actionOverload = false;         // action 重载
    String entryMethod = "index";           // 入口方法

    Consumer<Action> onActionCreated;       // action 创建后回调

    /**
     * 配置 action 重载，默认值为 false
     */
    public void setActionOverload(boolean actionOverload) {
        this.actionOverload = actionOverload;
    }

    /**
     * 配置入口方法。默认值 "index" 兼容 jfinal 使用习惯。
     */
    public void setEntryMethod(String entryMethod) {
        Objects.requireNonNull(entryMethod, "entryMethod can not be null.");
        this.entryMethod = entryMethod;
    }

    /**
     * 配置在 action 创建后的回调函数，便于扩展功能，例如实现 Mcp 服务端 Tool 注册功能，
     * 例如实现 permission 权限表内容生成与填充。
     *
     * <pre>
     *  例子：
     *    setOnActionCreated(action -> {
     *      // 根据 action 创建 Tool 并注册到 MCP 服务端
     *      mcpServer.registerTool(createToolFromAction(action));
     *    });
     * </pre>
     */
    public void setOnActionCreated(Consumer<Action> onActionCreated) {
        this.onActionCreated = onActionCreated;
    }

    /**
     * 获取整个 action 映射，用于生成权限管理所需的资源标识 action_key 数据，避免人工管理系统的权限资源。
     *
     * <pre>
     * 使用注意：
     *  1: Map 存放的 value 为 Action 与 ActionGroup，如果需要使用其内部数据需要通过 instanceof 自行判断并转换。
     *
     *  2: 如果 action 存在重载的情况，也就是 ActionGroup 的情况 action_key 是被多个 action 方法共享的，需妥善
     *     安排权限标识。
     *
     *  3: 建议参考本类中的路由匹配逻辑，在 ActionGroup 情况下，让 action 参数成为 action_key 的一部分，
     *     从而让 action_key 与 action 方法一一对应。
     *
     *  4: 如果确定 action 方法不存在重载情况，直接使用所有 action key 值即可，如：
     *         for (String actionKey : this.getActionMapping().keySet()) {
     *              // 这里使用 actionKey 生成权限标识
     *              // 并存入数据库的 permission 表的 action_key 字段
     *              // 用于实现权限管理，而不必手动管理 action_key 值
     *              // 可参考 jfinal-club 项目中类似的设计
     *         }
     *
     * </pre>
     */
    public Map<String, Object> getActionMapping() {
        return mapping;
    }

    /**
     * 手动添加路由避免类扫描。
     * 应用场景：对性能有极致要求，或者路由极少但 Class 文件却极多。
     */
    public void add(String targetPath, Class<?> target, Interceptor[] routesInterceptors) {
        injectRoutesInterceptor(routesInterceptors);
        buildRoute(targetPath, target, routesInterceptors);
    }

    private void injectRoutesInterceptor(Interceptor[] inters) {
        if (inters != null && AopKit.get().isInjectDependency()) {
            for (Interceptor inter : inters) {
                Aop.inject(inter);
            }
        }
    }

    /**
     * 扫描路由，为扫描到的路由添加 Routes 级别拦截器，Routes 拦截器调用优先级高于全局拦截器，
     * 通过 skip 函数跳过指定 Class。
     *
     * <p>
     * 设计备忘：basePackage 参数暗含 filter 逻辑，第三个参数必须设计成 skip 逻辑。
     */
    public void scan(String basePackage, Interceptor[] routesInterceptors, Predicate<Class<?>> skip) {
        injectRoutesInterceptor(routesInterceptors);

        Set<Class<?>> scannedSet = new Scanner().scan(basePackage, c -> c.isAnnotationPresent(Path.class));
        printScannedClass(scannedSet);
        scannedSet.removeAll(allScannedSet);        // 去重：若多次被扫描到，则拦截器配置以第一次被扫描时为准
        allScannedSet.addAll(scannedSet);           // 添加到大集合

        for (Class<?> target : scannedSet) {
            if (skip == null || !skip.test(target)) {
                String targetPath = target.getAnnotation(Path.class).value().trim();
                buildRoute(targetPath, target, routesInterceptors);
            }
        }
    }

    // 重复扫描到的 class 以第一次为准，此处输出警告日志
    private void printScannedClass(Set<Class<?>> currentScannedSet) {
        for (Class<?> t : currentScannedSet) {
            if (allScannedSet.contains(t)) {
                log.info("Class already scanned, skipping duplicate: " + t.getName());
            }
        }
    }

    /**
     * 针对一个 target class 构建路由
     */
    private void buildRoute(String targetPath, Class<?> target, Interceptor[] routesInterceptors) {
        if (!targetPath.startsWith(SLASH)) {
            throw new IllegalArgumentException("Class 级 @Path 注解值前缀必须为字符 '/'");
        }

        // Routes 级别拦截器
        Interceptor[] routesInters = routesInterceptors != null ? routesInterceptors : EMPTY_INTERS;

        // class 级别拦截器
        Interceptor[] classInters = InterceptorKit.get().createInterceptor(target);

        for (Method method : target.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.isAnnotationPresent(NoPath.class)) {
                continue;
            }

            Path methodPathAnno = method.getAnnotation(Path.class);
            String methodPath;
            if (methodPathAnno != null) {                       // @Path 注解值优先
                methodPath = methodPathAnno.value().trim();
            } else if (method.getName().equals(entryMethod)) {  // 兼容 jfinal index 入口方法
                methodPath = "";
            } else {
                methodPath = method.getName();
            }
            String actionPath = buildActionPath(targetPath, methodPath);

            Interceptor[] actionInters = InterceptorKit.get().buildAifeiInterceptor(routesInters, classInters, target, method);
            Argument<?, ?, ?>[] actionArgs = buildActionArgumentArray(target, method);
            Action action = new Action(actionPath, targetPath, actionInters, target, method, actionArgs);
            addAction(action);

            // action 创建后回调，便于扩展功能，如实现 Mcp 服务端 Tool 注册功能
            if (onActionCreated != null) {
                onActionCreated.accept(action);
            }
        }
    }

    /**
     * 构建 actionPath
     *
     * <pre>
     * actionPath 设计：
     *  1: 多数情况 actionPath = classPath + methodPath。
     *  2: methodPath 以 '/' 为前缀，表示绝对路径，将忽略 classPath 值。
     *  3: methodPath 为 "" 值，可实现 jfinal 类似 index() 方法为入口方法功能。
     * </pre>
     */
    private static String buildActionPath(String classPath, String methodPath) {
        // method 级 @Path 注解值以 '/' 为前缀表示绝对路径，忽略 classPath
        if (methodPath.startsWith(SLASH)) {
            return methodPath;
        }

        if (classPath.equals(SLASH)) {
            return classPath + methodPath;
        } else {
            // 必须判断 isEmpty()，否则出现 /user/ 这种路由
            return methodPath.isEmpty() ? classPath : classPath + SLASH + methodPath;
        }
    }

    private Argument<?,?,?>[] buildActionArgumentArray(Class<?> target, Method method) {
        int paraCount = method.getParameterCount();
        Argument<?,?,?>[] ret = new Argument[paraCount];
        Parameter[] parameterArray = method.getParameters();

        int pathParaIndex = 0;
        int matchCount = 0;
        for (int i = 0; i < paraCount; i++) {
            Argument<?,?,?> arg = buildActionArgument(target, method, parameterArray[i]);
            ret[i] = arg;

            if (arg.isMatch()) {
                // 参与路由匹配的参数数量（含 pathPara）。Argument 子类在其值为 1 且参数不存在时，可令 name="" 去获取数据
                matchCount++;

                if (arg.isPathPara()) {
                    // path parameter 赋予 index 值。默认值为 -1 不使用
                    arg.initIndex(pathParaIndex++);
                }
            }
        }

        // 设置参与路由匹配的参数数量。当前 Action 所有 Argument 的 matchCount 值相等
        for (int i = 0; i < paraCount; i++) {
            ret[i].initMatchCount(matchCount);
        }

        return ret;
    }

    /**
     * 构建 Action 所属的 Argument
     */
    private Argument<?,?,?> buildActionArgument(Class<?> targetClass, Method method, Parameter parameter) {
        if (!parameter.isNamePresent()) {
            log.warn("Parameter injection requires compiler option \"-parameters\" : " +
                    targetClass.getName() + "." + method.getName() + "(...) \n");
        }
        // Para paraAnn = parameter.getAnnotation(Para.class);
        // 检测可能的错误配置：Para 注解至少配置其中的一个，否则可以删掉
        // if (paraAnn != null && !paraAnn.path() && paraAnn.match() && paraAnn.name().equals(Para.UNSET) && paraAnn.defaultValue().equals(Para.UNSET)) {
        //     throw new IllegalArgumentException("The \"Para\" annotation should configure at least one of them: "
        //             + targetClass.getName() + "." + method.getName() + "(" + parameter.getName() + ")");
        // }

        try {
            Argument<?, ?, ?> argument = ArgumentKit.get().createArgument(parameter);
            argument.init(parameter);
            return argument;
        } catch (Exception e) {
            throw new RuntimeException("Argument creation failed: " + targetClass.getName() + "."
                    + method.getName() + "(...)", e);
        }
    }

    /**
     * 添加扫描到的 action
     */
    private void addAction(Action action) {
        String actionPath = action.getActionPath();
        Object prevAction = mapping.get(actionPath);
        if (prevAction == null) {
            mapping.put(actionPath, action);
            return;
        }

        if (!actionOverload) {
            Action prev = prevAction instanceof Action
                    ? (Action) prevAction
                    : ((ActionGroup) prevAction).getActionArray()[0];
            throw new IllegalStateException(
                    "Action overloading is not enabled by configuration. " +
                            "Conflict detected for path \"" + actionPath + "\": " +
                            "existing action = " + prev.getBriefInfo() + ", " +
                            "attempted to add = " + action.getBriefInfo()
            );
        }

        if (prevAction instanceof Action) {
            ActionGroup actionGroup = new ActionGroup();
            actionGroup.add((Action) prevAction);
            actionGroup.add(action);
            mapping.put(actionPath, actionGroup);
        } else {
            ActionGroup actionGroup = (ActionGroup) prevAction;
            actionGroup.add(action);
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * 匹配路由，获取 action。
     *
     * <pre>
     * 支持两种类型路由，规则为：
     *  1: 路由类型一：/targetPath/method
     *  2: 路由类型二：/targetPath/method/para
     *  3: targetPath 自身可以为字符 '/'，也可以包含一个或多个字符 '/'。例如：/method 与 /aaa/bbb/ccc/method
     *  4: method 可以通过 @Path("") 注解改为空字符串，所以路由也可以没有方法名：/targetPath 或者 /targetPath/para
     *     该设计与 jfinal 约定入口方法名为 index 的设计等价。
     *     method 也支持 jfinal 入口方法名为 index 的用法。
     *
     * path para 必须参与路由匹配：
     *  1: path 参数值几乎总是 "必须存在"（否则路由都匹配不了）。
     *  2: 命名参数（类似 query/form/body 的语义）大量是 "可缺省" 的，例如：筛选条件、分页字段、排序等等。
     * </pre>
     */
    public Action getAction(String path, Input input) {
        String pathPara = "";
        Object actionObject = mapping.get(path);
        if (actionObject == null) {
            int index = path.lastIndexOf('/');
            if (index != -1) {
                actionObject = mapping.get(path.substring(0, index));
                if (actionObject != null) {
                    pathPara = path.substring(index + 1);
                    if (!pathPara.isEmpty()) {
                        input.pathPara(pathPara);
                    }
                }
            }
        }

        // 处理 Action，进一步验证 pathPara。仅验证存在性不验证数量提升性能，符合大多数场景，提升开发体验
        if (actionObject instanceof Action) {
            Action action = (Action) actionObject;
            if (action.getPathParaCount() == 0) {
                return pathPara.isEmpty() ? action : null;
            } else {
                // 验证路径参数数量。缺点是依赖 has 方法且用户必须严格实现 has 方法（且无法验证：参数值是否为 null 或者 ""）
                // return input.has(action.getPathParaCount() - 1) ? action : null;

                // 验证路径参数存在性，不验证数量是否相等（且无法验证：传递数量 > 所需数量）
                return pathPara.isEmpty() ? null : action;
            }
        }

        // 处理 ActionGroup，进一步匹配参数
        if (actionObject instanceof ActionGroup) {
            return matchParameter((ActionGroup) actionObject, input);
        }

        return null;
    }

    /**
     * ActionGroup 需进一步匹配参数
     *
     * <pre>
     * ActionGroup 路由参数匹配设计：
     *  1: 原则：严格匹配参数个数、名称，避免匹配到错误 action，提升应用健壮性、安全性。
     *  2: 类型：不匹配类型，路由匹配时无法确定最终类型，因为调用 action 前可能进行类型转换，且后续 action 调用时类型不对会抛异常。
     *  3: 数量：参数数量必须满足 action 所需数量。
     *  4: 优先：优先匹配参数数量多的 action，即尽可能匹配更多参数，已在 ActionGroup 中按数量进行过排序。
     *  5: 返回：返回第一个成功匹配，未匹配成功则返回 null。
     *</pre>
     */
    private Action matchParameter(ActionGroup actionGroup, Input input) {
        Action[] actionArray = actionGroup.getActionArray();
        for (Action action : actionArray) {
            if (doMatchParameter(action, input)) {
                return action;
            }
        }

        return null;    // 对于 ActionGroup 未匹配成功返回 null，而非返回第一或最后一个
    }

    private boolean doMatchParameter(Action action, Input input) {
        int pathParaIndex = 0;
        Argument<?, ?, ?>[] arguments = action.getArguments();
        for (Argument<?, ?, ?> arg : arguments) {
            if (!arg.isMatch()) {                       // 跳过不参与匹配的参数
                continue;
            }

            if (arg.isPathPara()) {                     // 匹配路径参数
                if (input.has(pathParaIndex++)) {
                    continue;
                }
            } else {                                    // 匹配命名参数
                String argName = arg.getName();
                if (argName.isEmpty() || input.has(argName)) {
                    continue;
                }
            }

            return false;
        }

        return true;
    }
}


