# TODO

## Foundation: Spring Boot Backend

Status: complete for the current panel workflow.

- [x] Spring Boot 4.1, Java 25 target, Maven Wrapper.
- [x] CRUD/query APIs for experiments, configs, runs, results, metrics.
- [x] H2 local profile and PostgreSQL connection profile.
- [x] Swagger/OpenAPI.
- [x] Seed data and integration tests.

## Milestone 1: Read-only Evaluation Panel

- [x] Import the benchmark-owned Netagent schema 1.1 artifact directly.
- [x] Reject duplicate run ids and unsupported artifact shapes.
- [x] Store query summaries in the database and complete artifacts under a
  Workbench-owned root.
- [x] Expose filtered run summaries and composed run details.
- [x] Display final answers, metrics, scoring checks, and tool traces.
- [x] Cover the flow with a sanitized OSPF artifact fixture.

## Milestone 2: Launch Replay Benchmarks

- [x] Add a case catalog and reuse agent configurations as model profiles.
- [x] Add persistent asynchronous benchmark jobs.
- [x] Add a Netagent worker that claims jobs and reports heartbeats.
- [x] Launch one replay case from the panel and import its result.
- [x] Add cancellation, timeout, and explicit failure reporting.

## Milestone 3: Model Comparison

- [x] Run the same versioned case across selected model profiles.
- [x] Support repetitions and preserve configuration snapshots.
- [x] Compare pass rate, score, latency, tokens, and tool efficiency.

## Milestone 4: IE CML Execution

- [ ] Register an IE-network worker and validate read-only CML access.
- [ ] Add lab locking, baseline verification, and reset semantics.
- [ ] Enable write tasks only with explicit policy and post-change verification.

## Deployment Hardening

- [ ] Add operator and worker authentication before binding beyond localhost.
- [ ] Add production-like profile.
- [ ] Add PostgreSQL schema migrations before the first PostgreSQL deployment.
- [ ] Document PostgreSQL and artifact directory layout.
- [ ] Add CI for build and tests.
- [ ] Add minimal systemd examples only when deployment starts.
