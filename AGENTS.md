# AGENTS.md

## Project Shape

This repository is a single Spring Boot backend service.

- Java code lives under `src/main/java`.
- Tests live under `src/test`.
- FYP service examples live under `contracts/fyp-agent-service`.
- Keep documentation limited to `README.md`, `AGENTS.md`, and `TODO.md`.

## Development Rules

- Use Java 21 target and Spring Boot 3.5.x.
- Prefer small, focused changes that match the existing controller/service/repository pattern.
- Do not introduce Docker Desktop as a local development requirement.
- Do not commit secrets, internal data, production logs, or unsanitized FYP artifacts.
- Keep FYP execution code in the adjacent FYP repository; this service stores metadata and normalized results.

## Verification

Run tests before handing off code changes:

```bash
./mvnw test
```

For API checks, start the app and use Swagger:

```bash
./mvnw spring-boot:run
```
