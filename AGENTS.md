# Repository Guidelines

## Project Structure & Module Organization

This is a Java 8 multi-module Maven project rooted at `pom.xml`. Modules are:

- `aifei`: core framework APIs and runtime.
- `aifei-log`, `aifei-json`, `aifei-proxy`, `aifei-db`, `aifei-enjoy`, `aifei-undertow`: focused feature modules.
- `aifei-all`: shaded all-in-one distribution; avoid primary source here unless packaging requires it.

Production code lives under each module's `src/main/java`, with packages under `cn.aifei`. Resources use `src/main/resources`; `.af` templates under `src/main/java` are also included by the parent build. No test tree is present yet; add tests under the relevant module's `src/test/java`.

## Build, Test, and Development Commands

- `mvn clean install`: builds all modules, runs tests, and installs artifacts locally.
- `mvn -pl aifei-db test`: runs tests for one module.
- `mvn -pl aifei-db -am test`: also builds required upstream modules.
- `mvn -pl aifei-all package`: creates the shaded all-in-one artifact.
- `mvn -Pcentral-release verify`: performs signed release verification and Central validation.

## Coding Style & Naming Conventions

Use Java 8 only. Keep UTF-8 encoding and 4-space indentation. Follow existing package organization and place new classes near the feature they extend, for example `cn.aifei.db.sql` for SQL directives or `cn.aifei.server.undertow` for Undertow integration. Use `UpperCamelCase` for classes, `lowerCamelCase` for methods and fields, and module names matching Maven artifact IDs.

Prefer small, dependency-light APIs consistent with the framework's minimal design. Keep comments useful and short; match nearby English or Chinese comments when editing.

## Testing Guidelines

Use Maven's standard test lifecycle. Add unit tests beside the changed module under `src/test/java`, with names like `SqlKitTest` or `UndertowConfigTest`. Cover parser, SQL, reflection, proxy, and server behavior with focused tests. Run the changed module's tests before submitting, and run `mvn clean install` for cross-module changes.

## Commit & Pull Request Guidelines

Recent history uses Conventional Commit-style subjects such as `docs: fix typo in comment` and `refactor: relax List parameter generic type`. Use a short imperative subject with a clear type, for example `fix: handle null SQL parameter`.

Pull requests should describe the behavior change, list affected modules, include test results, and link issues. For public API changes, document compatibility impact and update `README.md` when usage changes.

## Security & Configuration Tips

Do not commit IDE files, build output, logs, or agent state; `.gitignore` already excludes `.idea/`, `target/`, `.codex/`, and `.claude/`. Keep credentials, signing keys, database URLs, and Central publishing tokens outside the repository.
