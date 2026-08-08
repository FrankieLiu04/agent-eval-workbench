# agent-eval-workbench

Evaluation panel and Spring Boot control plane for launching and inspecting
Netagent benchmark runs. The adjacent `netagent-benchmark` repository owns
agent execution and scoring; this service queues work, imports its sanitized
`run.json`, and presents the complete trace as experiment evidence.

## Stack

- Java 25 target
- Spring Boot 4.1.x
- Spring Data JPA
- H2 local profile
- PostgreSQL profile for later deployment
- Swagger/OpenAPI
- Server-hosted HTML, CSS, and JavaScript panel with no frontend build step

## Layout

```text
agent-eval-workbench/
├── src/main/java/          # Spring Boot API, import, and persistence
├── src/main/resources/     # Profiles and the static evaluation panel
├── src/test/               # Java/JavaScript tests and schema 1.1/1.2 fixtures
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
node --test src/test/javascript/*.test.cjs
./mvnw spring-boot:run
```

Open:

- Evaluation panel: <http://localhost:8080/>
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
/api/v1/benchmark-cases
/api/v1/benchmark-jobs
/api/v1/benchmark-jobs/batches
/api/v1/benchmark-jobs/batches/{batchId}/comparison
/api/v1/evaluation-runs
/api/v1/evaluation-results
/api/v1/run-metrics
/api/v1/imports/netagent-run-json
```

Use Swagger for full request and response details.

## Compare Benchmark Profiles

Build both applications, start Workbench, and then start the optional Netagent
worker in another terminal:

```bash
cd ../netagent-benchmark
mise exec -- mvn package
java -jar target/netagent-benchmark-0.1.0-SNAPSHOT.jar worker
```

Open <http://localhost:8080/>, select an experiment and case, choose one or more
model profiles, and set the repetition count. One batch creates
`profiles x repetitions` persistent `QUEUED` jobs. Workers claim them normally,
so comparison does not introduce a second execution path.

The default comparison matrix contains five supported DeepSeek V4 profiles:
`deepseek-v4-flash` at `LOW`, `HIGH`, and `MAX`, plus `deepseek-v4-pro` at
`HIGH` and `MAX`. The job snapshot freezes the profile name, model, prompt
version, tool exposure, max turns, reasoning mode, and case schema version
before the first worker claim.

The comparison table keeps capability, reliability, and efficiency separate.
It reports benchmark pass rate and average score, attempted-run reliability,
tokens, agent steps, duration, and duplicate-tool-call rate. Execution failures
and timeouts reduce reliability but are not rewritten as benchmark grading.
The run detail preserves the complete trajectory as evidence.

The seeded `Local Mock Case Reference` profile runs the deterministic OSPF
replay without an API key. The five DeepSeek profiles use `DEEPSEEK_API_KEY`
from the worker process environment; Workbench never stores or sends the key.

Run a worker for at most one claim:

```bash
java -jar target/netagent-benchmark-0.1.0-SNAPSHOT.jar worker --once
```

Queued jobs can be cancelled immediately. Running cancellation is delivered on
the next heartbeat. The worker also enforces each job timeout, while Workbench
marks expired timeouts and worker leases explicitly.

## Import A Netagent Run

The request body is an unchanged Netagent schema 1.1 or 1.2 artifact. Schema
1.2 adds optional `agent_steps` and `duplicate_tool_calls` efficiency metrics;
missing schema 1.1 values remain unavailable rather than being treated as zero.
The `experimentId` query parameter supplies the Workbench context that is not
part of the benchmark-owned artifact.

```bash
curl -X POST \
  'http://localhost:8080/api/v1/imports/netagent-run-json?experimentId=2' \
  -H 'Content-Type: application/json' \
  --data-binary '@/path/to/netagent-benchmark/experiments/runs/<run>/run.json'
```

Workbench stores searchable run, score, and metric summaries in the database.
It copies the complete artifact into
`./data/artifacts/<run-id>/run.json`; configure another root with
`ARTIFACT_ROOT`.

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
- Keep the panel focused on benchmark launch, evidence inspection, and model
  comparison rather than turning it into a generic dashboard platform.
- Keep the current control plane on its default loopback address. Add operator
  and worker authentication before binding it to another interface.
- Do not commit secrets, internal data, production logs, or unsanitized network
  artifacts.

## Maintenance

- Use `TODO.md` for milestones and pending work.
- Use `AGENTS.md` for agent-specific repo instructions.
- Avoid adding new documentation files unless there is a clear long-term need.
- CI runs JavaScript regression tests and the Maven `verify` lifecycle on Java
  25 for pull requests and pushes to `main`.
