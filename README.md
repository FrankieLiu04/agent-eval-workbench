# Network Agent Workbench

Network Agent Workbench is a Java full-stack side project for experimenting with network diagnostic agents, synthetic troubleshooting scenarios, and human-readable run reports.

This repository starts as a clean monorepo skeleton. It intentionally does not include generated Spring Boot or React application code yet.

## Repository Layout

```text
network-agent-workbench/
├── contracts/      # API contracts and sample payloads
├── backend/        # Future Java backend service
├── frontend/       # Future web UI
├── mock-agent/     # Future synthetic diagnostic agent harness
├── deploy/         # Local deployment helpers
└── docs/           # Architecture, API, and demo notes
```

## Current Scope

- Define the initial workspace shape.
- Keep all sample data synthetic.
- Avoid committing credentials, tokens, private company material, or real network data.
- Leave framework scaffolding for later focused milestones.

## Quick Start

At this stage there is no runnable application. Review the planning and contract documents:

- [plan.md](plan.md)
- [docs/architecture.md](docs/architecture.md)
- [docs/api-contract.md](docs/api-contract.md)
- [docs/demo-cases.md](docs/demo-cases.md)

## Security Notes

- Do not commit real API keys, GitHub tokens, LLM keys, CML credentials, internal documents, or production diagnostic logs.
- Use `.env.example` as a template only.
- Keep mock data synthetic and safe for public discussion, even though this repository is private.
