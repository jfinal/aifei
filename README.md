# Aifei

世界首个 AI Coding 框架。

## 简介

Aifei 是世界首个 AI Coding 框架。

Aifei 开创 Just Service 开发范式，面向 AI Coding 设计，从结构层面减少 Token 消耗、缩短上下文长度、提升 Attention 浓度，极大提升代码生成质量、生成稳定性与开发体验。

Aifei 采用极简设计，内核仅 3333 行代码且无第三方依赖，将极简推至全新高度。从 JFinal 到 Aifei，专注极简设计 15 年。

Aifei 大幅消除传统框架中被视为必需的概念，如 Controller、Render、Repository、Mapper，极小化认知负荷与上下文噪音，极大化模型注意力浓度。

Just Service. Only Aifei can do.

## 为什么是 Aifei

过去三年，AI 写代码的能力快速跃升，代码生成正从辅助走向主导。

围绕 AI Coding 的模型、IDE 与各类工具快速发展，但从框架层面为 AI Coding 而设计的服务端 framework 仍然缺位。

现有主流框架几乎都诞生于**人类手写代码**的时代，它们服务的是人类开发者的认知方式、组织方式与工程习惯。

但 AI 并不需要这些历史包袱。AI 更需要的是：

- 更少的 Token 消耗
- 更高的 Attention 浓度
- 更低的上下文噪音
- 更稳定的生成模式
- 更直接的业务表达方式

现有框架并未围绕这些核心需求构建。Aifei 正是为此而生。

## 快速上手

### Service

Just Service 开发范式之下，在框架搭好之后，模型只需专注生成业务代码。

在 AI Coding 场景中，这意味着模型只需围绕单一 Service 层生成代码，无需在 Controller、Service、Dao、Repository、Mapper、Render 等多层结构之间进行映射、拆分与补全，从而显著减少 Token 消耗，降低上下文噪音，并提升生成稳定性与代码质量：

```java
@Path("/vip")
public class VipService {

    // 查询、排序、分页
    public Page<Vip> index(Map<?, ?> filter, int pageNum, int pageSize) {
        String sql = "select * from vip #where(...) #and(...) ...";
        return Vip.sql(sql, filter).paginate(pageNum, pageSize);
    }

    // 插入
    public Out insert(Vip vip) {
        vip.insert();
        return Out.ok("插入成功");
    }
    
    // 修改
    public Vip edit(int id) {
        return Vip.findById(id);
    }

    // 更新
    public Out update(Vip vip) {
        vip.update();
        return Out.ok("更新成功");
    }

    // 删除
    public Out delete(int id) {
        Vip.deleteById(id);
        return Out.ok("删除成功");
    }
}
```

### 配置

Aifei 配置在 AifeiConfig 接口中集中管理。

集中式配置避免了分散式约定、隐式行为与多入口配置带来的上下文噪音，使 AI 在生成、理解与修改项目结构时更稳定、更直接、更可预测。

以 VIP 订阅会员项目配置为例：

```java
/**
 * Aifei 配置中心
 */
public class AppConfig implements AifeiConfig<In, Out> {

    Prop p;

    /**
     * 偏好配置
     */
    public void config(Settings<In, Out> settings) {
        // 加载配置
        p = PropKit.use("app-config.txt");

        // 配置日志
        settings.setLogFactory(new Log4j2LogFactory());

        // 配置 Server、Dispatcher、Handler
        settings.setServer(new UndertowServer(), new IoDispatcher());
        settings.addHandler(new IoHandler());

        // 定制 action 注入参数，注入登录账号
        settings.configArgument(kit -> {
            kit.register(Account.class, LoginAccountArgument.class);
        });
    }

    /**
     * 路由配置
     */
    public void config(Routes routes) {
        routes.scan("cn.aifei.vip", new AuthInterceptor());
    }

    /**
     * 插件配置
     */
    public void config(Plugins plugins) {
        DruidSupplier druidSupplier = new DruidSupplier(p.get("jdbcUrl"), p.get("user"), p.get("password"));
        AifeiDbPlugin dbPlugin = new AifeiDbPlugin("main", druidSupplier);
        dbPlugin.addModelSet(new ModelSet());
        dbPlugin.config(c -> c.setPrintSql(true));
        plugins.add(dbPlugin);
    }
}
```

### 启动

main 方法中调用 Aifei.start(...) 即可启动：

```java
public class AifeiVip {
    public static void main(String[] args) {
        Aifei.start(new AppConfig(), args);
    }
}
```

### HIO

Aifei 顶层采用 HIO 结构，即 Handler、Input、Output 组成的处理模型。

该模型将请求处理流程收敛为明确、稳定且可预测的结构，有助于 AI 在生成代码时形成一致模式，减少理解成本与结构歧义。

三者均由用户自行定义，不依赖 Servlet，可按需切换底层 IO 实现。

## 核心理念

### Just Service

Just Service 是 Aifei 成为 AI Coding 框架的核心。

它消除了传统框架中的 Controller、Render、Dao、Repository、Mapper 等非业务性结构，将代码收敛为单一的 Service 层。

这一设计并非只是**更少代码**，而是直接作用于大模型的工作机制：

- 显著减少 Token 消耗，缩短上下文长度
- 提升注意力浓度，让模型聚焦于业务语义
- 降低上下文噪音，减少无关结构对生成的干扰
- 提供更稳定的生成模式，避免结构复杂导致输出波动

在传统框架中，大量分层结构与样板代码会占据上下文预算，稀释模型对核心业务的关注。而 Just Service 将上下文尽可能留给业务逻辑本身。

在框架搭好之后，AI 只需围绕 Service 生成代码，无需在多层结构之间来回映射与补全，从而获得更直接、更稳定的生成结果。

例如：

```java
@Path("/user")
public class UserService {
    public User getById(int id) {
        return User.findById(id);
    }
}
```

在 Web 场景下，以上代码可通过访问 `/user?id=...` 直接调用。

注：Aifei 是通用服务端框架，不限定于 Web 系统，Web 只是应用场景之一。

## 数据库访问

### 开始

Aifei 的数据库模块继承了 jfinal 数据库模块的大部分核心 API 与使用体验。

从 API 使用层面看，Aifei 相比 jfinal 的主要变化在于：查询改为链式调用，并新增 `sql(...)` 方法统一承载 SQL 与参数。

这一设计并非只是 API 风格变化，而是将数据库访问收敛为统一、稳定、可预测的生成模式：

- SQL 始终通过单一入口表达
- 参数传递方式始终保持一致
- 调用结构不再分散

这使 AI 在生成数据库代码时，无需在多种写法之间选择，从而减少结构分叉、降低上下文复杂度，并提升生成稳定性。

深入底层实现，Aifei 数据库模块采用全新架构，代码量缩减近 50%，进一步减少理解成本与 Token 消耗。

### Db + Row 模式

Aifei db 的所有数据库操作通过 Db + Row 模式实现。

这一模式将数据访问收敛为单一抽象，避免 DAO、Mapper、Entity 等多套体系并存带来的结构分裂问题。

在 AI Coding 场景中，这意味着：

- 所有数据操作路径一致
- 无需在多种访问模型之间切换
- 上下文中只存在一套数据操作语义

从而显著降低上下文噪音，并帮助模型形成稳定的生成模式。

### Model

Aifei db 并不提供独立的 Model 层，而是将 Model 视为一种 Row。

这一设计减少了抽象层级，使数据访问不再存在 Row / Model / DTO 等多重语义切换。

对于大模型来说，这意味着：

- 更少的概念切换
- 更短的上下文路径
- 更稳定的代码生成结构

```
    // User 是一种 Row，而非新增抽象。操作语义与 Row 共享
    User.of(123).delete();

    User.of(456).name("james").update();
```

### 插入

一行代码即可完成：

```
    // 通过 Row 插入数据
    Row.of("user").set("name", "james").insert();
    
    // 通过 Model 插入数据
    new User().name("james").insert();
```

### 删除

一行代码即可完成：

```
    // 通过 Db 删除
    Db.deleteById("user", 123);
    
    // 通过 Row 删除
    Row.of("user").id(123).delete();
    
    // 通过 Model 删除
    User.deleteById(123);
```

### 修改

一行代码即可完成：

```
    // 通过 Row 修改
    Row.of("user").id(123).set("name", "james").update();
    
    // 通过 Model 修改
    User.of(123).name("james").update();
```

### 查询

Aifei 将查询统一为“SQL + 链式调用”的固定结构：

```
    // SQL + 链式 API
    String sql = "select ... from ... #where(...) #and(...) #orderBy(...)";
    Db.sql(sql, filter).find();
    
    // 同一 SQL 不同 API 共享
    Db.sql(sql, filter).paginate(1, 30);
```

这一结构具有明确边界：

- SQL 负责表达查询语义
- API 负责控制执行方式

两者职责清晰，不混合、不分散，使 AI 在生成查询代码时无需推断隐式行为。

更多示例：

```
    // 查询第一个
    Db.sql("select * from user").findFirst();

    // 查询一个，未查到时抛异常
    Db.sql("select * from user").findOne();

    // 分页，并对每页进行迭代。适合对全表进行全量操作
    User.sql("select * from user").forEachPage(10, (Page<User> page) -> {
        for (User user : page.getRows()) {
            System.out.println(user.getName());
        }
        return true;    // 返回 false 终止于当前分页
    });
```

统一的调用形式，使模型更容易复用生成结果，减少结构偏差。

查询条件生成指令 #where 与 #and：

```
    // where 指令与 and 指令只在参数不为 null 时生成 SQL
    select * from user #where(name, '=', name)

    // 完美替代以往 jfinal 用法
    select * from user where 1 = 1 #if(name) and name = #para(name) #end
```

#where 与 #and 指令，将动态 SQL 从“模板拼接”转化为“结构化表达”。

对于 AI 来说，这一变化非常关键：

- 不再需要生成分支结构（if / end）
- 不再需要维护模板语法一致性
- 条件表达变为固定函数式结构

从而显著降低生成复杂度，并减少出错概率。

#where 与 #and 指令参数用法完全一样：

```
    select ... #where(name, 'like', name) #and(age, '>', age)
```

排序指令 #orderBy：

```
    // 前端传递 orderBy 变量
    {
       name: 'james',
       orderBy: { field: 'updated', order: 'desc' }
    }

    // 后端接收参数并使用
    public List<User> search(Map<?, ?> filter) {
        // orderBy 指令参数 created、id 为可用于排序的白名单，用于防止 SQL 注入
        String sql = "select * from user #where(name, '=', name) #orderBy(created, id)";
        return User.sql(sql, filter).find();
    }
```

#orderBy 指令将排序生成收敛为白名单约束下的固定模式：

- 排序字段受限于白名单
- 排序结构固定
- 无需拼接字符串

这不仅提升安全性，也使 AI 在生成排序逻辑时无需构造复杂表达，从而提升生成稳定性。

### 事务

```
    // id 为 1 的账号转账 100 元到 id 为 2 的账号
    Db.transaction(tx -> {
        int money = 100;
        int n1 = Db.sql("update account set money = money - ? where id = ? and money >= ?", money, 1, money).update();
        int n2 = Db.sql("update account set money = money + ? where id = ?", money, 2).update();
        return n1 == 1 && n2 == 1 ? Out.ok("转账成功") : Out.fail("转账失败");
    });
```

### 批量操作

Aifei 的批量操作支持 Row / Model 参数名、参数数量都不相同的场景，完美解决 jfinal 遗留问题：

```
    // 批量插入
    List<Row> list = new ArrayList<>();
    list.add(new Row().set("age", 11));
    list.add(new Row().set("name", "测试123"));
    Db.batch().insert("user", list);

    // 批量更新
    List<Row> list = new ArrayList<>();
    list.add(new Row().set("id", 1).set("age", 18));         // 更新 age
    list.add(new Row().set("id", 2).set("name", "测试456"));  // 更新 name
    Db.batch().update("user", list);
```

Aifei 的批量操作支持参数结构不一致的场景。

这使 AI 在处理复杂数据时：

- 无需对数据结构做额外归一化
- 无需引入分支逻辑处理差异
- 仍可保持统一生成模式

从而避免生成复杂度随数据结构增长而急剧上升。

批量操作在 Excel 导入等场景下是刚需，该功能相对 jfinal 有极大提升。

### 完整性

Aifei db 功能完整覆盖 jfinal 数据库功能，但代码量缩减近 50%，在降低 Token 消耗的同时，将数据库操作体验提升到新的高度。

## AI Coding

AI Coding 是 Aifei 的核心应用场景。

Aifei 并非在传统框架之上增加 AI 用法，而是从框架结构本身出发，直接围绕大模型的工作机制重新思考服务端开发。

Just Service 将代码结构压缩至最小，使大模型在生成代码时可以将绝大部分上下文预算用于业务逻辑本身，而不是消耗在框架结构、分层映射与样板代码上。

相比传统框架，Aifei 具备以下优势：

- 更少的 Token 消耗
- 更短的上下文长度
- 更高的注意力浓度
- 更低的上下文噪音
- 更稳定的生成模式
- 更统一的结构表达

在实际使用中，大模型只需围绕 Service、SQL 与数据生成代码，即可完成完整业务逻辑，实现真正意义上的 AI Coding。

后续官方会逐步推出 AI Coding 相关资源，如 skills。

当下需要体验的同学，可以让大模型参考样例代码直接生成业务逻辑。Just Service 之下，已可获得稳定且高质量的生成结果。

## 更多内容

为尽早发布，当前文档优先覆盖核心理念与快速上手。HIO、AI Coding 实践、完整工程示例等内容将在后续持续补充。

如需提前体验更完整的企业级实现，可订阅官方唯一 VIP 会员：[Aifei VIP 订阅](https://aifei.cn/vip)

