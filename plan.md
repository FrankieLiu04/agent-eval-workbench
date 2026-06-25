# Network Agent Workbench Plan

## Goal

Build a compact Java full-stack workbench for running synthetic network diagnostic agent cases, inspecting reasoning steps, and comparing agent outputs with expected findings.

## Milestones

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

## Non-Goals For Initialization

- No generated backend or frontend framework code yet.
- No real network telemetry.
- No real credentials.
- No integration with company systems.
