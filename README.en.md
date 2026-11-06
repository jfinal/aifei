# Aifei

[简体中文](README.md) | **English**

The world's first AI Coding framework.

Website: https://aifei.cn

Documentation: https://aifei.cn/doc

Demo: https://gitee.com/jfinal/aifei-eco/tree/main/aifei-demo

Ecosystem: https://gitee.com/jfinal/aifei-eco

## Introduction

Aifei is the world's first AI Coding framework.

Aifei introduces the Just Service development paradigm, designed for AI Coding. Its structure reduces token consumption, shortens context length, and keeps the model's attention focused, greatly improving code generation quality, consistency, and the development experience.

Aifei takes minimalism to a new level: its core contains just 3,333 lines of code and has no third-party dependencies. From JFinal to Aifei, minimal design has been the focus for 15 years.

Aifei eliminates many concepts traditionally considered essential in frameworks, such as Controller, Render, Repository, and Mapper, minimizing cognitive load and context noise while maximizing the model's focus.

Just Service. Only with Aifei.

## Why Aifei?

Over the past three years, AI's ability to write code has advanced rapidly, and code generation is moving from a supporting role to a leading one. The era of writing all code by hand is coming to an end, with developers shifting toward decision-making and review.

Models, IDEs, and other tools for AI Coding are evolving quickly, yet a server-side framework designed for AI Coding at the framework level has been missing.

Traditional frameworks were designed for the era of handwritten code. Aifei is designed for the era of AI Coding.

For AI Coding, frameworks need to prioritize what large language models need from code structure:

- Lower token consumption
- More focused attention
- Less context noise
- A higher context signal-to-noise ratio
- More consistent generation patterns
- More direct expression of business logic

A framework is no longer just a tool for humans to organize code. It also provides the context structure that large language models use to understand, generate, and maintain code. Existing frameworks were not built around these core needs. Aifei was created to address them.

## Quick Start

### Service

With the Just Service development paradigm, once the framework is set up, the model only needs to focus on generating business logic.

For AI Coding, this means generating code within a single Service layer, without mapping, splitting, and filling in code across Controller, Service, Dao, Repository, Mapper, Render, and other layers. This significantly reduces token consumption and context noise while improving generation consistency and code quality:

```java
@Path("/vip")
public class VipService {

    // Query, sort, and paginate
    public Page<Vip> index(Map<?, ?> filter, int pageNum, int pageSize) {
        String sql = "select * from vip #where(...) #and(...) ...";
        return Vip.sql(sql, filter).paginate(pageNum, pageSize);
    }

    // Insert
    public Out insert(Vip vip) {
        vip.insert();
        return Out.ok("Inserted successfully");
    }

    // Edit
    public Vip edit(int id) {
        return Vip.findById(id);
    }

    // Update
    public Out update(Vip vip) {
        vip.update();
        return Out.ok("Updated successfully");
    }

    // Delete
    public Out delete(int id) {
        Vip.deleteById(id);
        return Out.ok("Deleted successfully");
    }
}
```

A Service created by `InstanceFactory` must have a `public` no-argument constructor accessible to the factory. The factory does not override access restrictions. The example above declares no constructor, so Java provides a default `public` no-argument constructor for this `public` class. If you declare a constructor with parameters, you must explicitly retain a `public` no-argument constructor.

By default, the factory prefers lambda-based object creation. You can call `InstanceFactory.setJit(false)` at startup to use standard reflection instead. This setting applies globally to `InstanceFactory` and does not affect the JVM's JIT compilation.

### Configuration

Aifei configuration is centralized in the `AifeiConfig` interface.

Centralized configuration avoids the context noise caused by scattered conventions, implicit behavior, and multiple configuration entry points. This makes generating, understanding, and modifying the project structure more consistent, direct, and predictable for AI.

Here is a configuration example for a VIP subscription project:

```java
/**
 * Central Aifei configuration
 */
public class AppConfig implements AifeiConfig<In, Out> {

    Prop p;

    /**
     * General settings
     */
    public void config(Settings<In, Out> settings) {
        // Load configuration
        p = PropKit.use("app-config.txt");

        // Configure logging
        settings.setLogFactory(new Log4jLogFactory());

        // Configure Server, Dispatcher, and Handler
        settings.setServer(new UndertowServer(), new IoDispatcher());
        settings.addHandler(new IoHandler());

        // Customize action argument injection to inject the logged-in account
        settings.configArgument(kit -> {
            kit.register(Account.class, LoginAccountArgument.class);
        });
    }

    /**
     * Route configuration
     */
    public void config(Routes routes) {
        routes.scan("cn.aifei.vip", new AuthInterceptor());
    }

    /**
     * Plugin configuration
     */
    public void config(Plugins plugins) {
        DruidSupplier ds = new DruidSupplier(p.get("jdbcUrl"), p.get("user"), p.get("password"));
        AifeiDbPlugin dbPlugin = new AifeiDbPlugin("main", ds);

        dbPlugin.setDialect(new MysqlDialect());    // Configure the dialect
        dbPlugin.addModelSet(new ModelSet());       // Add the generated model set
        dbPlugin.setPrintSql(true);                 // Print SQL for debugging and optimization
        dbPlugin.setFormatSql(false);               // Configure SQL formatting
        // dbPlugin.setPrintSqlToLog(true);         // Write SQL to a log file to identify and optimize slow queries

        plugins.add(dbPlugin);
    }
}
```

### Startup

Call `Aifei.start(...)` in the `main` method to start the application:

```java
public class AifeiVip {
    public static void main(String[] args) {
        Aifei.start(new AppConfig(), args);
    }
}
```

### HIO

Aifei uses HIO, an architecture built around a processing model of Handler, Input, and Output that gives you control over the processing flow and data structures.

Handler controls the processing flow, while Input and Output define the input and output data structures.

This model gives request processing a clear, stable, and predictable structure, helping AI generate consistent code while reducing the effort needed to understand it and avoiding structural ambiguity.

You define all three components yourself. They do not depend on Servlet, and you can switch the underlying I/O implementation as needed.

## Core Concepts

### Just Service

Just Service is what makes Aifei an AI Coding framework.

It eliminates structures unrelated to business logic, such as Controller, Render, Dao, Repository, and Mapper, bringing code into a single Service layer.

This design is about more than **less code**. It directly addresses how large language models work:

- Significantly reduces token consumption and shortens context length
- Keeps attention focused on business semantics
- Reduces context noise and interference from irrelevant structures during generation
- Provides more consistent generation patterns, avoiding output variability caused by structural complexity

In traditional frameworks, extensive layering and boilerplate consume the context budget and dilute the model's focus on core business logic. Just Service reserves as much context as possible for the business logic itself.

Once the framework is set up, AI only needs to generate code around Service, without repeatedly mapping and filling in code across multiple layers. The result is more direct and consistent code generation.

For example:

```java
@Path("/user")
public class UserService {
    public User getById(int id) {
        return User.findById(id);
    }
}
```

In a web application, you can invoke the code above directly by visiting `/user?id=...`.

Note: Aifei is a general-purpose server-side framework. It is not limited to web systems; the web is just one of its use cases.

### Context Rot

Context rot is a hidden bottleneck in AI Coding. Although large language models have increasingly long context windows, **being able to fit information** does not mean **being able to use it reliably**. As context grows, models do not use all the information it contains consistently, evenly, or without loss.

In traditional frameworks, structures unrelated to business logic, such as Controller, Dao, Repository, Mapper, and Render, along with boilerplate, repeated configuration, historical context, and irrelevant files, continually consume the context budget and dilute the model's attention to the current business objective.

The key to AI Coding is not simply giving models larger context windows. It is designing project structures with an inherently higher context signal-to-noise ratio and more focused attention. In other words, a framework should optimize the **effective context window**, rather than just the nominal one. By reducing structures unrelated to business logic, Just Service reserves as much context as possible for the business logic itself.

## Database Access

### Getting Started

Aifei's database module inherits most of the core APIs and developer experience of JFinal's database module.

From an API usage perspective, the main changes from JFinal are method chaining for queries and a new `sql(...)` method that brings SQL and its parameters together.

This is more than a change in API style. It gives database access a uniform, stable, and predictable generation pattern:

- SQL is always expressed through a single entry point
- Parameters are always passed in the same way
- Calls follow a consistent structure

When generating database code, AI no longer needs to choose among multiple styles. This reduces structural variation and context complexity while improving generation consistency.

Under the hood, Aifei's database module uses an entirely new architecture with nearly 50% less code, further reducing the effort needed to understand it and the tokens it consumes.

### The Db + Row Pattern

All database operations in Aifei db use the Db + Row pattern.

This pattern brings data access into a single abstraction, avoiding the structural fragmentation caused by the coexistence of DAO, Mapper, Entity, and other approaches.

For AI Coding, this means:

- All data operations follow a consistent path
- There is no need to switch between different access models
- The context contains a single set of data operation semantics

This significantly reduces context noise and helps the model establish consistent generation patterns.

### Model

Aifei db does not provide a separate Model layer. Instead, it treats Model as a kind of Row.

This reduces the number of abstraction layers and removes the need to switch between Row, Model, DTO, and other semantics during data access.

For large language models, this means:

- Fewer conceptual switches
- Shorter paths through the context
- A more consistent structure for code generation

```java
    // User is a kind of Row, not an additional abstraction. It shares Row's operation semantics.
    User.of(123).delete();

    User.of(456).name("james").update();
```

### Insert

All it takes is one line of code:

```java
    // Insert data using Row
    Row.of("user").set("name", "james").insert();

    // Insert data using Model
    new User().name("james").insert();
```

### Delete

All it takes is one line of code:

```java
    // Delete using Db
    Db.deleteById("user", 123);

    // Delete using Row
    Row.of("user").id(123).delete();

    // Delete using Model
    User.deleteById(123);
```

### Update

All it takes is one line of code:

```java
    // Update using Row
    Row.of("user").id(123).set("name", "james").update();

    // Update using Model
    User.of(123).name("james").update();
```

### Queries

Aifei gives queries a uniform structure: SQL plus method chaining.

```java
    // SQL + a fluent API
    String sql = "select ... from ... #where(...) #and(...) #orderBy(...)";
    Db.sql(sql, filter).find();

    // Reuse the same SQL with different API methods
    Db.sql(sql, filter).paginate(1, 30);
```

This structure has clear boundaries:

- SQL expresses the query semantics
- The API controls how the query is executed

The responsibilities are clearly separated and kept in one place, so AI does not need to infer implicit behavior when generating query code.

More examples:

```java
    // Find the first result
    Db.sql("select * from user").findFirst();

    // Find one result; throw an exception if none is found
    Db.sql("select * from user").findOne();

    // Paginate and iterate over each page. Useful for processing an entire table.
    User.sql("select * from user").forEachPage(10, (Page<User> page) -> {
        for (User user : page.getRows()) {
            System.out.println(user.getName());
        }
        return true;    // Return false to stop after the current page
    });
```

A uniform call structure makes it easier for models to reuse generated code and reduces structural inconsistencies.

The `#where` and `#and` directives generate query conditions:

```sql
    // The where and and directives generate SQL only when the parameter is not null
    select * from user #where(name, '=', name)

    // Replaces the previous JFinal approach shown below
    select * from user where 1 = 1 #if(name) and name = #para(name) #end
```

The `#where` and `#and` directives turn dynamic SQL from template concatenation into structured expressions.

For AI, this is a significant change:

- No need to generate branching structures (`if` / `end`)
- No need to maintain template syntax consistency
- Conditions follow a fixed, function-like structure

This significantly reduces generation complexity and the likelihood of errors.

The `#where` and `#and` directives accept parameters in exactly the same way:

```sql
    select ... #where(name, 'like', name) #and(age, '>', age)
```

The `#orderBy` directive handles sorting:

```
    // The frontend passes an orderBy variable
    {
       name: 'james',
       orderBy: { field: 'updated', order: 'desc' }
    }

    // The backend receives and uses the parameters
    public List<User> search(Map<?, ?> filter) {
        // The orderBy arguments created and id form an allowlist of sortable fields to prevent SQL injection
        String sql = "select * from user #where(name, '=', name) #orderBy(created, id)";
        return User.sql(sql, filter).find();
    }
```

The `#orderBy` directive gives sorting a fixed pattern constrained by an allowlist:

- Sort fields are restricted to the allowlist
- The sorting structure is fixed
- No string concatenation is needed

This improves security and lets AI generate sorting logic without constructing complex expressions, making code generation more consistent.

### Transactions

```java
    // Transfer 100 yuan from the account with id 1 to the account with id 2
    Db.transaction(tx -> {
        int money = 100;
        int n1 = Db.sql("update account set money = money - ? where id = ? and money >= ?", money, 1, money).update();
        int n2 = Db.sql("update account set money = money + ? where id = ?", money, 2).update();
        return n1 == 1 && n2 == 1 ? Out.ok("Transfer successful") : Out.fail("Transfer failed");
    });
```

### Batch Operations

Aifei's batch operations support Row / Model objects with different parameter names and counts, resolving a longstanding limitation in JFinal:

```java
    // Batch insert
    List<Row> list = new ArrayList<>();
    list.add(new Row().set("age", 11));
    list.add(new Row().set("name", "test123"));
    Db.batchInsert("user", list);

    // Batch update
    List<Row> list = new ArrayList<>();
    list.add(new Row().set("id", 1).set("age", 18));        // Update age
    list.add(new Row().set("id", 2).set("name", "test456")); // Update name
    Db.batchUpdate("user", list);
```

Aifei's batch operations support inconsistent parameter structures.

When AI works with complex data, this means:

- No additional normalization of data structures is needed
- No branching logic is needed to handle differences
- A uniform generation pattern can still be maintained

This prevents generation complexity from rising sharply as data structures grow more complex.

Batch operations are essential for use cases such as Excel imports. This feature is a major improvement over JFinal.

### Feature Coverage

Aifei db covers all of JFinal's database functionality with nearly 50% less code, reducing token consumption while taking the database development experience to a new level.

## AI Coding

AI Coding is Aifei's core use case.

Aifei rethinks server-side development around how large language models work, starting with the framework's structure itself, rather than adding AI usage patterns on top of a traditional framework.

Just Service reduces code structure to a minimum, allowing large language models to spend most of their context budget on business logic when generating code, instead of framework structure, mappings between layers, and boilerplate.

Compared with traditional frameworks, Aifei offers:

- Lower token consumption
- Shorter context length
- More focused attention
- Less context noise
- More consistent generation patterns
- More uniform structural expression

In practice, large language models can implement complete business logic by generating code around Service, SQL, and data, making AI Coding a reality.

Official AI Coding resources, such as skills, will be released over time.

To try it now, ask a large language model to generate business logic using the example code as a reference. With Just Service, you can already achieve consistent, high-quality results.

## Build and Test

Use a full JDK (including `javac`) and Maven to build the project and run all tests from the project root:

```bash
mvn clean install
```

To test only the modules related to object creation and proxies:

```bash
mvn -pl aifei,aifei-enjoy,aifei-proxy -am clean test
```

To verify compatibility with JDK 8, 17, 21, and 25, point `JAVA_HOME` to each JDK in turn and run the test commands above. The tests cover both lambda and reflection modes, constructor access restrictions, exception propagation, class loader isolation, and proxy behavior. JPMS tests are skipped on JDK 8 and check module exports and readability on JDK 9 and later.

## More to Come

To make Aifei available sooner, the current documentation focuses on core concepts and getting started. More content on HIO, AI Coding practices, complete project examples, and other topics will be added over time.

For early access to a more complete enterprise implementation, subscribe to the only official VIP membership: [Aifei VIP Subscription](https://aifei.cn)
