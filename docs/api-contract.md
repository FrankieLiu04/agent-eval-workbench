# API Contract

Language: [English](#english) | [中文](#中文)

## English

### POST /api/agent-runs

Submit a synthetic network diagnostic case and receive an agent run result.

#### Request

See `contracts/samples/sample-agent-run-request.json`.

Key fields:

- `caseId`: stable synthetic case identifier.
- `target`: synthetic service, environment, and region metadata.
- `symptoms`: user-visible symptoms to investigate.
- `observations`: structured synthetic measurements.
- `constraints`: execution limits for the agent.

#### Response

See `contracts/samples/sample-agent-run-response.json`.

Key fields:

- `runId`: unique run identifier.
- `status`: run lifecycle status.
- `summary`: concise conclusion.
- `confidence`: numeric confidence from 0 to 1.
- `steps`: investigation timeline.
- `findings`: actionable findings and recommendations.

### Future Endpoints

- `GET /api/agent-runs/{runId}`: fetch a previous run.
- `GET /api/demo-cases`: list built-in synthetic demo cases.
- `POST /api/agent-runs/{runId}/feedback`: capture user feedback.

## 中文

### POST /api/agent-runs

提交一个合成网络诊断 case，并接收 agent run result。

#### Request

参考 `contracts/samples/sample-agent-run-request.json`。

关键字段：

- `caseId`：稳定的合成 case identifier。
- `target`：合成 service、environment 和 region metadata。
- `symptoms`：需要调查的用户可见症状。
- `observations`：结构化的合成 measurements。
- `constraints`：agent 的执行限制。

#### Response

参考 `contracts/samples/sample-agent-run-response.json`。

关键字段：

- `runId`：唯一 run identifier。
- `status`：run lifecycle status。
- `summary`：简洁结论。
- `confidence`：0 到 1 之间的数值置信度。
- `steps`：调查 timeline。
- `findings`：可执行的发现和建议。

### Future Endpoints

- `GET /api/agent-runs/{runId}`：获取之前的 run。
- `GET /api/demo-cases`：列出内置合成 demo cases。
- `POST /api/agent-runs/{runId}/feedback`：记录用户反馈。
