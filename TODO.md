# TODO

## Milestone 1: Spring Boot Backend MVP

Status: mostly complete.

- [x] Spring Boot 4.1, Java 25 target, Maven Wrapper.
- [x] CRUD/query APIs for experiments, configs, runs, results, metrics.
- [x] H2 local profile and PostgreSQL profile.
- [x] Swagger/OpenAPI.
- [x] Seed data and integration tests.
- [ ] Add broader PUT/DELETE and validation test coverage.

## Milestone 2: FYP Artifact Ingestion

- [x] Define sanitized `run.json` import payload.
- [x] Add import API for FYP run artifacts.
- [x] Normalize imported data into run/result/metric tables.
- [x] Keep artifact references instead of storing large logs in DB columns.
- [ ] Add basic run comparison API.

## Milestone 3: FYP Agent Service Integration

- [ ] Add Java HTTP client for the FYP Agent Service.
- [ ] Add run submission API.
- [ ] Persist remote FYP run id and status.
- [ ] Poll status and store returned artifacts/results.
- [ ] Add timeout and error mapping.

## Milestone 4: Deployment Hardening

- [ ] Add production-like profile.
- [ ] Document PostgreSQL and artifact directory layout.
- [ ] Add CI for build and tests.
- [ ] Add minimal systemd examples only when deployment starts.

## Milestone 5: Optional Dashboard

- [ ] Add only after backend ingestion and FYP integration are useful.
