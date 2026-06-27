# AGENTS.md

## Project Shape

`agent-eval-workbench` is a single Spring Boot backend service.

- Java code lives under `src/main/java`.
- Tests live under `src/test`.
- FYP service examples live under `contracts/fyp-agent-service`.
- Keep documentation limited to `README.md`, `AGENTS.md`, and `TODO.md`.

## Development Rules

- Use the newest installed Java LTS target for this project; currently use Java
  25 and Spring Boot 4.1.x. Do not downgrade for unrelated local projects.
  使用本项目可用的最新 Java LTS；当前使用 Java 25 和 Spring Boot 4.1.x。不要为了无关本地项目降级。
- Prefer current stable dependency releases that support that LTS and Spring
  Boot line. Major upgrades are acceptable only when tests pass and migration
  notes fit in the same diff.
  优先使用支持该 LTS 和 Spring Boot 线的稳定依赖版本；大版本升级必须测试通过，并在同一 diff 中说明迁移点。
- Prefer small, focused changes that match the existing controller/service/repository pattern.
- Do not introduce Docker Desktop as a local development requirement.
- Do not commit secrets, internal data, production logs, or unsanitized FYP artifacts.
- Keep FYP execution code in the adjacent FYP repository; this service stores metadata and normalized results.
- Treat copied legacy `.env` files as untrusted input: keep only variables that
  this service reads, mirror safe placeholders in `.env.example`, and never log
  or document real secret values.
  将旧项目复制来的 `.env` 视为待清理输入：只保留本服务实际读取的变量，在 `.env.example` 中同步安全占位值，不记录真实密钥。
- Keep Maven, IDE, and JDK caches in their normal user/global locations. Do not
  download dependencies into the repo and hide them with `.gitignore`.
  Maven、IDE 和 JDK 缓存使用本机用户/全局位置；不要把依赖下载到仓库后再用 `.gitignore` 隐藏。

## Long-Term Build Style

- Optimize for fast iteration and human readability: implement the direct
  workflow first, and add abstractions only after the controller/service/repo
  boundary would otherwise become harder to read.
  面向快速迭代和人类可读性：先实现直接工作流；只有当 controller/service/repo 边界会变难读时才抽象。
- Keep feature changes small: target one API or workflow behavior per diff, and keep most diffs under 300 changed lines unless schema fixtures or generated files are involved.
  保持改动小而聚焦：每个 diff 默认只解决一个 API 或工作流行为，除 schema fixture 或生成文件外，尽量控制在 300 行变更以内。
- Keep methods short: target <= 35 logical lines per method; split only when the extracted method has a clear domain name and is reused or independently testable.
  保持方法短小：单个方法目标不超过 35 行逻辑代码；只有当提取方法有清晰领域命名、可复用或可独立测试时才拆分。
- Keep classes focused: controllers route requests, services own workflow logic, repositories own persistence; if a class grows beyond about 250 lines, check whether it mixes these roles.
  保持类职责聚焦：controller 负责请求路由，service 负责流程逻辑，repository 负责持久化；如果类超过约 250 行，检查是否混合职责。
- Add tests only for explicit behavior: prefer 1-3 focused tests per changed behavior, and avoid broad regression suites unless a previous bug or public API contract makes them necessary.
  只为明确行为添加测试：每个变更行为优先写 1-3 个聚焦测试，除非历史 bug 或公开 API 契约需要，否则避免宽泛回归套件。
- Avoid redundant safety code: do not add fallback branches, broad exception
  handlers, compatibility shims, or regression-only tests unless they guard a
  named API contract or an observed failure.
  避免冗余兜底代码：不要添加 fallback、宽泛异常处理、兼容 shim 或纯回归测试，除非它们保护明确 API 契约或已观察失败。
- Keep mocks minimal: mock only external systems, clocks, or nondeterministic boundaries; avoid mocking internal service calls just to assert implementation details.
  保持 mock 最小化：只 mock 外部系统、时间或非确定性边界；避免为了断言实现细节而 mock 内部 service 调用。
- Require justification for fallback code: every retry, catch-all exception handler, default substitution, or degraded mode must name the concrete failure it handles.
  兜底代码必须说明理由：每个 retry、兜底异常处理、默认替换或降级模式都要写明它处理的具体失败。
- Use bilingual comments sparingly: write only for non-obvious assumptions, contracts, or migration reasons; use `// EN:` and `// CN:` in Java and `# EN:` and `# CN:` in Markdown/config docs.
  谨慎使用中英对照注释：只写非显然假设、契约或迁移原因；Java 用 `// EN:` 和 `// CN:`，Markdown/配置文档用 `# EN:` 和 `# CN:`。
- Do not confuse Java text blocks with comments: `"""..."""` in Java is a
  multiline string literal and is allowed for JSON/SQL/test payloads. Comment
  text still uses `// EN:` / `// CN:` or compact Javadoc only when necessary.
  不要把 Java text block 当成注释：Java 中的 `"""..."""` 是多行字符串字面量，可用于 JSON/SQL/测试 payload；注释仍使用 `// EN:` / `// CN:`。
- Keep bilingual comments concise: each line should normally fit within about
  100 characters and explain why the code exists, not restate method names,
  annotations, or obvious assignments.
  保持双语注释简洁：每行通常不超过约 100 字符，只解释原因，不复述方法名、注解或直白赋值。
- Before coding, write down the intended behavior, touched files, expected tests, and any threshold exception. After coding, review the diff against the same four items before handoff.
  写代码前写清目标行为、涉及文件、预期测试和任何阈值例外；写完后交付前用同四项复查 diff。

## Environment Checks

- Verify Java with `java -version` before debugging build failures; this project
  expects Java 25 in the local shell and Maven process.
  调试构建失败前先用 `java -version` 核对；本项目期望本地 shell 和 Maven 进程使用 Java 25。
- If Maven sees an older runtime, set `JAVA_HOME=/opt/homebrew/opt/openjdk@25`
  for the command or update the IDE Project SDK to the same path.
  如果 Maven 看到旧运行时，命令中设置 `JAVA_HOME=/opt/homebrew/opt/openjdk@25`，或把 IDE Project SDK 指向同一路径。

## Verification

Run tests before handing off code changes:

```bash
./mvnw test
```

For API checks, start the app and use Swagger:

```bash
./mvnw spring-boot:run
```
