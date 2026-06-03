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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InterceptorKit.
 *
 * <pre>
 * InterceptorKit 设计：
 *    1：管理控制层、业务层全局拦截器
 *    2：缓存业务层 Class 级拦截器数组。业务层拦截器被整体缓存在 ProxyMethod 中
 *    3：用于创建 Interceptor、组装 Interceptor
 *    4：除手动 new 出来的拦截器以外，其它所有拦截器均为单例
 *
 * 无法使用 Method 或 Before 对象缓存业务层 Method 级拦截器：
 *    1：不同对象或相同对象获取同一个 Class 中同一个 Method 得到的对象 id 值不相同
 *    2：不同对象获取同一个 method 之上的 Before 得到的对象 id 值不相同
 * </pre>
 */
public class InterceptorKit {

    private static final Interceptor[] EMPTY_INTERS = new Interceptor[0];

    // 控制层与业务层全局拦截器
    private Interceptor[] globalActionInters = EMPTY_INTERS;
    private Interceptor[] globalServiceInters = EMPTY_INTERS;

    // 业务层 Class 级别拦截器缓存
    private final ConcurrentHashMap<Class<?>, Interceptor[]> serviceClassInters = new ConcurrentHashMap<>(128);

    // 单例拦截器
    private final ConcurrentHashMap<Class<? extends Interceptor>, Interceptor> singletonMap = new ConcurrentHashMap<>(256);

    private InterceptorKit() {}
    static InterceptorKit kit = new InterceptorKit();
    public static InterceptorKit get() {return kit;}

    // 此处不缓存 target Class 级拦截器，已经在 cn.aifei.router.Action 对象中缓存
    public Interceptor[] createInterceptor(Class<?> targetClass) {
        return createInterceptor(targetClass.getAnnotation(Before.class));
    }

    // 此处不缓存控制层 Class 级拦截器，已经在 com.jfinal.core.Action 对象中缓存
    // public Interceptor[] createControllerInterceptor(Class<?> controllerClass) {
    //     return createInterceptor(controllerClass.getAnnotation(Before.class));
    // }

    // 缓存业务层 Class 级拦截器
    public Interceptor[] createServiceInterceptor(Class<?> serviceClass) {
        Interceptor[] result = serviceClassInters.get(serviceClass);
        if (result == null) {
            result = createInterceptor(serviceClass.getAnnotation(Before.class));
            serviceClassInters.put(serviceClass, result);
        }
        return result;
    }

    /**
     * aifei 不再限定是否存在 controller，不再分为 "控制层全局拦截器" 与 "业务层全局拦截器"，统一为 "全局拦截器"
     *
     * 确定：routes 拦截器在 global 拦截器之前调用：
     *      1: routes 拦截器用得最多的场景是登录验证
     *      2: global 拦截器用于业务层时，如果依赖登录验证，就必须让 routes 拦截器先执行
     */
    public Interceptor[] buildAifeiInterceptor(Interceptor[] routesInters, Interceptor[] classInters, Class<?> targetClass, Method method) {
        //  routesInters 放 globalServiceInters 之前
        return doBuild(routesInters, globalServiceInters, classInters, targetClass, method);
        // return doBuild(globalServiceInters, routesInters, classInters, targetClass, method);
    }

    // public Interceptor[] buildActionInterceptor(Interceptor[] routesInters, Interceptor[] classInters, Class<?> targetClass, Method method) {
    //     return doBuild(globalActionInters, routesInters, classInters, targetClass, method);
    // }

    public Interceptor[] buildServiceMethodInterceptor(Class<?> serviceClass, Method method) {
        return doBuild(globalServiceInters, EMPTY_INTERS, createServiceInterceptor(serviceClass), serviceClass, method);
    }

    private Interceptor[] doBuild(Interceptor[] globalInters, Interceptor[] routesInters, Interceptor[] classInters, Class<?> targetClass, Method method) {
        Interceptor[] methodInters = createInterceptor(method.getAnnotation(Before.class));

        Class<? extends Interceptor>[] clearIntersOnMethod;
        Clear clearOnMethod = method.getAnnotation(Clear.class);
        if (clearOnMethod != null) {
            clearIntersOnMethod = clearOnMethod.value();
            if (clearIntersOnMethod.length == 0) {	// method 级 @Clear 且不带参
                return methodInters;
            }
        } else {
            clearIntersOnMethod = null;
        }

        Class<? extends Interceptor>[] clearIntersOnClass;
        Clear clearOnClass = targetClass.getAnnotation(Clear.class);
        if (clearOnClass != null) {
            clearIntersOnClass = clearOnClass.value();
            if (clearIntersOnClass.length == 0) {	// class 级 @clear 且不带参
                globalInters = EMPTY_INTERS;
                routesInters = EMPTY_INTERS;
            }
        } else {
            clearIntersOnClass = null;
        }

        ArrayList<Interceptor> result = new ArrayList<Interceptor>(globalInters.length + routesInters.length + classInters.length + methodInters.length);
        for (Interceptor inter : globalInters) {
            result.add(inter);
        }
        for (Interceptor inter : routesInters) {
            result.add(inter);
        }
        if (clearIntersOnClass != null && clearIntersOnClass.length > 0) {
            removeInterceptor(result, clearIntersOnClass);
        }
        for (Interceptor inter : classInters) {
            result.add(inter);
        }
        if (clearIntersOnMethod != null && clearIntersOnMethod.length > 0) {
            removeInterceptor(result, clearIntersOnMethod);
        }
        for (Interceptor inter : methodInters) {
            result.add(inter);
        }
        return result.toArray(new Interceptor[result.size()]);
    }

    private void removeInterceptor(ArrayList<Interceptor> target, Class<? extends Interceptor>[] clearInters) {
        for (Iterator<Interceptor> it = target.iterator(); it.hasNext();) {
            Interceptor curInter = it.next();
            if (curInter != null) {
                Class<? extends Interceptor> curInterClass = curInter.getClass();
                for (Class<? extends Interceptor> ci : clearInters) {
                    if (curInterClass == ci) {
                        it.remove();
                        break;
                    }
                }
            } else {
                it.remove();
            }
        }
    }

    public Interceptor[] createInterceptor(Before beforeAnnotation) {
        if (beforeAnnotation == null) {
            return EMPTY_INTERS;
        }
        return createInterceptor(beforeAnnotation.value());
    }

    public Interceptor[] createInterceptor(Class<? extends Interceptor>[] interceptorClasses) {
        if (interceptorClasses == null || interceptorClasses.length == 0) {
            return EMPTY_INTERS;
        }

        Interceptor[] result = new Interceptor[interceptorClasses.length];
        try {
            for (int i=0; i<result.length; i++) {
                result[i] = singletonMap.get(interceptorClasses[i]);
                if (result[i] == null) {
                    // 此处不能使用 Aop.get(...)，避免生成代理类
                    result[i] = interceptorClasses[i].getDeclaredConstructor().newInstance();
                    if (AopKit.get().isInjectDependency()) {
                        Aop.inject(result[i]);
                    }
                    singletonMap.put(interceptorClasses[i], result[i]);
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addGlobalActionInterceptor(Interceptor... inters) {
        addGlobalInterceptor(true, inters);
    }

    public void addGlobalServiceInterceptor(Interceptor... inters) {
        addGlobalInterceptor(false, inters);
    }

    private synchronized void addGlobalInterceptor(boolean forAction, Interceptor... inters) {
        if (inters == null || inters.length == 0) {
            throw new IllegalArgumentException("interceptors can not be null.");
        }

        for (Interceptor inter : inters) {
            if (inter == null) {
                throw new IllegalArgumentException("interceptor can not be null.");
            }
            if (singletonMap.containsKey(inter.getClass())) {
                throw new IllegalArgumentException("interceptor already exists, interceptor must be singleton, do not create more then one instance of the same Interceptor Class.");
            }
        }

        for (Interceptor inter : inters) {
            if (AopKit.get().isInjectDependency()) {
                Aop.inject(inter);
            }
            singletonMap.put(inter.getClass(), inter);
        }

        Interceptor[] globalInters = forAction ? globalActionInters : globalServiceInters;
        Interceptor[] temp = new Interceptor[globalInters.length + inters.length];
        System.arraycopy(globalInters, 0, temp, 0, globalInters.length);
        System.arraycopy(inters, 0, temp, globalInters.length, inters.length);

        if (forAction) {
            globalActionInters = temp;
        } else {
            globalServiceInters = temp;
        }
    }

    public java.util.List<Class<?>> getGlobalServiceInterceptorClasses() {
        ArrayList<Class<?>> ret = new ArrayList<>(globalServiceInters.length + 3);
        for (Interceptor i : globalServiceInters) {
            ret.add(i.getClass());
        }
        return ret;
    }
}





