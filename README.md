# Network Agent Workbench

Language: [English](#english) | [中文](#中文)

## English

Network Agent Workbench is a Java full-stack side project for experimenting with network diagnostic agents, synthetic troubleshooting scenarios, and human-readable run reports.

This repository starts as a clean monorepo skeleton. It intentionally does not include generated Spring Boot or React application code yet.

### Repository Layout

```text
network-agent-workbench/
├── contracts/      # API contracts and sample payloads
├── backend/        # Future Java backend service
├── frontend/       # Future web UI
├── mock-agent/     # Future synthetic diagnostic agent harness
├── deploy/         # Local deployment helpers
└── docs/           # Architecture, API, and demo notes
```

### Current Scope

- Define the initial workspace shape.
- Keep all sample data synthetic.
- Avoid committing credentials, tokens, private company material, or real network data.
- Leave framework scaffolding for later focused milestones.

### Quick Start

At this stage there is no runnable application. Review the planning and contract documents:

- [plan.md](plan.md)
- [docs/architecture.md](docs/architecture.md)
- [docs/api-contract.md](docs/api-contract.md)
- [docs/demo-cases.md](docs/demo-cases.md)

### Security Notes

- Do not commit real API keys, GitHub tokens, LLM keys, CML credentials, internal documents, or production diagnostic logs.
- Use `.env.example` as a template only.
- Keep mock data synthetic and safe for public discussion, even though this repository is private.

## 中文

Network Agent Workbench 是一个 Java 全栈 side project，用于实验网络诊断 agent、合成故障排查场景，以及面向人的运行报告。

当前仓库从干净的 monorepo 骨架开始。初始化阶段暂不生成 Spring Boot 或 React 应用代码。

### 仓库结构

```text
network-agent-workbench/
├── contracts/      # API contract 和样例 payload
├── backend/        # 后续 Java backend service
├── frontend/       # 后续 Web UI
├── mock-agent/     # 后续合成诊断 agent harness
├── deploy/         # 本地部署辅助文件
└── docs/           # 架构、API、demo 说明
```

### 当前范围

- 定义初始 workspace 形态。
- 所有样例数据保持为合成数据。
- 避免提交凭据、token、公司资料或真实网络数据。
- 框架脚手架留到后续独立里程碑处理。

### 快速开始

当前阶段还没有可运行应用。可以先阅读计划和 contract 文档：

- [plan.md](plan.md)
- [docs/architecture.md](docs/architecture.md)
- [docs/api-contract.md](docs/api-contract.md)
- [docs/demo-cases.md](docs/demo-cases.md)

### 安全说明

- 不要提交真实 API key、GitHub token、LLM key、CML 凭据、内部文档或生产诊断日志。
- `.env.example` 只作为模板使用。
- 即使仓库是 private，mock data 也应保持合成，并适合公开讨论。
