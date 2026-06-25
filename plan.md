# Network Agent Workbench Plan

Language: [English](#english) | [中文](#中文)

## English

### Goal

Build a compact Java full-stack workbench for running synthetic network diagnostic agent cases, inspecting reasoning steps, and comparing agent outputs with expected findings.

### Milestones

1. Repository initialization
   - Create monorepo skeleton.
   - Add initial contracts and documentation.
   - Publish as a private GitHub repository.

2. Contract-first prototype
   - Stabilize request and response JSON shapes.
   - Add mock-agent CLI or lightweight service.
   - Add backend contract tests.

3. Java backend
   - Scaffold Spring Boot service.
   - Implement agent run submission and result retrieval.
   - Store synthetic run history locally.

4. Frontend workbench
   - Scaffold web UI.
   - Add run form, timeline view, findings table, and raw JSON inspection.

5. Local deployment
   - Add docker-compose services.
   - Document local environment setup.

### Non-Goals For Initialization

- No generated backend or frontend framework code yet.
- No real network telemetry.
- No real credentials.
- No integration with company systems.

## 中文

### 目标

构建一个轻量 Java 全栈 workbench，用于运行合成网络诊断 agent case、查看推理步骤，并将 agent 输出与预期发现进行对比。

### 里程碑

1. 仓库初始化
   - 创建 monorepo 骨架。
   - 添加初始 contract 和文档。
   - 发布为 GitHub private repository。

2. Contract-first prototype
   - 稳定 request 和 response JSON 结构。
   - 添加 mock-agent CLI 或轻量服务。
   - 添加 backend contract tests。

3. Java backend
   - 搭建 Spring Boot service。
   - 实现 agent run 提交和结果查询。
   - 在本地保存合成 run history。

4. Frontend workbench
   - 搭建 Web UI。
   - 添加 run form、timeline view、findings table 和 raw JSON inspection。

5. 本地部署
   - 添加 docker-compose services。
   - 编写本地环境设置文档。

### 初始化阶段暂不做

- 暂不生成 backend 或 frontend framework code。
- 不使用真实网络 telemetry。
- 不使用真实凭据。
- 不接入公司系统。
