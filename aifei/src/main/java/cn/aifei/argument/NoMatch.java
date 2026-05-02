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
 * 标识 Argument 实现类所支持参数的类型不参与路由匹配，与注解 @Para(match=false) 等价，
 * 实现该接口可省去对注解 @Para(match=false) 的使用
 *
 * <pre>
 * 例如：
 *    // User 实现 NoMatch 接口，后续作为 action 参数时将不参与路由匹配。
 *    public class User implements NoMatch {
 *        ...
 *    }
 *
 *    // User 未实现 NoMatch 接口的用法
 *    public void doService(Integer id, @Para(match=false) User loginUser) {
 *        ...
 *    }
 *
 *    // User 实现 NoMatch 接口之后的用法
 *    public void doService(Integer id, User loginUser) {
 *        ...
 *    }
 *
 * 注：User 所对应的 UserArgument 实现 NoMatch 效果一样：
 *    // UserArgument 实现 NoMatch 也可以令 User 不参与路由匹配
 *    public class UserArgument extends Argument<Input, Output, User> implements NoMatch {
 *       ...
 *    }
 * </pre>
 */
public interface NoMatch {

}

