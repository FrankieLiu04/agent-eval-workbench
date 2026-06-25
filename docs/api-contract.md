# API Contract

## POST /api/agent-runs

Submit a synthetic network diagnostic case and receive an agent run result.

### Request

See `contracts/samples/sample-agent-run-request.json`.

Key fields:

- `caseId`: stable synthetic case identifier.
- `target`: synthetic service, environment, and region metadata.
- `symptoms`: user-visible symptoms to investigate.
- `observations`: structured synthetic measurements.
- `constraints`: execution limits for the agent.

### Response

See `contracts/samples/sample-agent-run-response.json`.

Key fields:

- `runId`: unique run identifier.
- `status`: run lifecycle status.
- `summary`: concise conclusion.
- `confidence`: numeric confidence from 0 to 1.
- `steps`: investigation timeline.
- `findings`: actionable findings and recommendations.

## Future Endpoints

- `GET /api/agent-runs/{runId}`: fetch a previous run.
- `GET /api/demo-cases`: list built-in synthetic demo cases.
- `POST /api/agent-runs/{runId}/feedback`: capture user feedback.
