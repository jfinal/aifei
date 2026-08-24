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
 * NoMatch 用于标识 Argument 子类所针对的参数类型不参与路由匹配，与注解 @Para(match=false) 等价，
 * 实现该接口可省去对注解 @Para(match=false) 的使用
 *
 * <pre>
 * NoMatch 设计：
 *
 * 1: 路由开启 action 重载之后，其 action 参数才会参与路由匹配，NoMatch 才会被用到。
 *    未开启 action 重载时 action path 与 action 是一对一对关系，参数无需参与路由匹配。
 *    action 重载默认配置为 false，所以 NoMatch 默认无需关注。
 *
 *    action 重载开启配置：Router.setActionOverload(boolean actionOverload)
 *
 *    简言之：只有在 Router.setActionOverload(true) 之后才需关心 NoMatch。
 *
 * 2: Argument 子类实现 NoMatch 接口。建议优先使用此用法。
 *
 *      // UserArgument 实现 NoMatch 接口之后，其所针对的 User 类型不参与路由匹配
 *      public class UserArgument extends Argument<Input, Output, User> implements NoMatch {
 *          ...
 *      }
 *
 *      // User 作为 action 参数时将不再参与路由匹配
 *      \@Path("/user")
 *      public class UserService {
 *          public Out deleteOrder(int orderId, User user) {
 *              ...
 *          }
 *      }
 *
 * 3: 被注入类型直接实现 NoMatch 接口
 *
 *      // User 直接实现 NoMatch 接口，后续作为 action 参数时将不再参与路由匹配
 *      public class User implements NoMatch {
 *          ...
 *      }
 *
 * 4: 未实现 NoMatch 接口的用法，且希望不参与路由匹配需要使用注解 @Para(match=false)
 *
 *    // Account 未实现 NoMatch 接口，使用 @Para(match=false) 注解不参与路由匹配
 *    public void doService(Integer id, @Para(match=false) Account account) {
 *        ...
 *    }
 * </pre>
 */
public interface NoMatch {

}

