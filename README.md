# agent-eval-workbench

Java Spring Boot backend for tracking agent experiments, benchmark runs,
evaluation results, and metrics. The adjacent FYP repository owns agent
execution; this service stores metadata and exposes REST APIs.

## Stack

- Java 21 target
- Spring Boot 3.5.x
- Spring Data JPA
- H2 local profile
- PostgreSQL profile for later deployment
- Swagger/OpenAPI

## Layout

```text
agent-eval-workbench/
├── src/main/java/          # Spring Boot application code
├── src/main/resources/     # Spring profiles and config
├── src/test/               # tests
├── contracts/fyp-agent-service/
├── AGENTS.md
├── TODO.md
├── pom.xml
└── README.md
```

## Local Development

Docker Desktop is not required.

```bash
./mvnw test
./mvnw spring-boot:run
```

Open:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- H2 console: <http://localhost:8080/h2-console>
- Health: <http://localhost:8080/actuator/health>

H2 console:

```text
JDBC URL: jdbc:h2:file:./data/agent-workbench
User: sa
Password:
```

## API Surface

```text
/api/v1/experiments
/api/v1/agent-configs
/api/v1/evaluation-runs
/api/v1/evaluation-results
/api/v1/run-metrics
/api/v1/imports/fyp-run-json
```

Use Swagger for full request and response details.

## Profiles

Local H2:

```bash
./mvnw spring-boot:run
```

PostgreSQL:

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/agent_workbench \
DATABASE_USERNAME=agent_workbench \
DATABASE_PASSWORD=agent_workbench \
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Boundaries

- Keep this repository as a single Spring Boot backend service.
- Keep FYP execution code in the adjacent FYP repository.
- Keep frontend work optional until backend ingestion and FYP integration are
  useful.
- Do not commit secrets, internal data, production logs, or unsanitized network
  artifacts.

## Maintenance

- Use `TODO.md` for milestones and pending work.
- Use `AGENTS.md` for agent-specific repo instructions.
- Avoid adding new documentation files unless there is a clear long-term need.
