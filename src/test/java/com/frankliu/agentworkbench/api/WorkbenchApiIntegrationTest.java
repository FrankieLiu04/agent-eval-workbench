package com.frankliu.agentworkbench.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkbenchApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions postJson(String path, String payload) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload));
    }

    private long createExperiment(String name) throws Exception {
        String response = postJson("/api/v1/experiments", """
                {
                  "name": "%s",
                  "domain": "NETWORK_BENCHMARK",
                  "description": "Created by MockMvc test",
                  "datasetName": "synthetic-api-dataset",
                  "baselineModel": "deepseek-v4-flash",
                  "status": "ACTIVE"
                }
                """.formatted(name))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonTestSupport.extractLong(response, "id");
    }

    private long createAgentConfig(String name) throws Exception {
        String response = postJson("/api/v1/agent-configs", """
                {
                  "name": "%s",
                  "provider": "DEEPSEEK",
                  "modelName": "deepseek-v4-flash",
                  "promptVersion": "test_prompt_v1",
                  "toolExposure": "all",
                  "maxSteps": 12,
                  "reasoningMode": "DEFAULT"
                }
                """.formatted(name))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonTestSupport.extractLong(response, "id");
    }

    @Test
    void seedDataIsQueryable() throws Exception {
        mockMvc.perform(get("/api/v1/experiments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].id").exists());

        mockMvc.perform(get("/api/v1/evaluation-runs")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void createExperimentConfigRunResultAndMetric() throws Exception {
        long experimentId = createExperiment("API-created benchmark");
        long configId = createAgentConfig("API-created config");

        String runResponse = postJson("/api/v1/evaluation-runs", """
                {
                  "experimentId": %d,
                  "agentConfigId": %d,
                  "source": "MANUAL",
                  "task": "Run a synthetic benchmark sample",
                  "status": "COMPLETED",
                  "artifactPath": "../netagent-benchmark/experiments/api-test/run.json"
                }
                """.formatted(experimentId, configId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.experimentId").value(experimentId))
                .andExpect(jsonPath("$.agentConfigId").value(configId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long runId = JsonTestSupport.extractLong(runResponse, "id");

        postJson("/api/v1/evaluation-results", """
                {
                  "runId": %d,
                  "score": 0.91,
                  "accuracy": 0.91,
                  "successCount": 10,
                  "failCount": 1,
                  "wrongCount": 0,
                  "formatErrorCount": 0,
                  "syntaxErrorCount": 0,
                  "testErrorCount": 0,
                  "summary": "API-created evaluation result"
                }
                """.formatted(runId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value(runId))
                .andExpect(jsonPath("$.accuracy").value(0.91));

        postJson("/api/v1/run-metrics", """
                {
                  "runId": %d,
                  "latencyMs": 1234,
                  "promptTokens": 200,
                  "completionTokens": 80,
                  "totalTokens": 280,
                  "toolCallCount": 3,
                  "mutatingToolCallCount": 1,
                  "failedToolCallCount": 0
                }
                """.formatted(runId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value(runId))
                .andExpect(jsonPath("$.totalTokens").value(280));
    }

    @Test
    void importFypRunJsonCreatesRunResultAndMetric() throws Exception {
        long experimentId = createExperiment("FYP import benchmark");

        postJson("/api/v1/imports/fyp-run-json", """
                {
                  "experimentId": %d,
                  "workbench_import": {
                    "schema_version": "1.0",
                    "run_id": "20260626T010000Z-api-import",
                    "timestamp": "2026-06-26T01:00:00Z",
                    "task": "debug bgp neighbor"
                  },
                  "benchmark": {
                    "name": "fyp-agent",
                    "case_id": "20260626T010000Z-api-import",
                    "level": "agent-run"
                  },
                  "agent": {
                    "provider": "openai-compatible",
                    "model": "deepseek-v4-flash",
                    "reasoning_mode": "default"
                  },
                  "result": {
                    "status": "completed",
                    "score": 0.82,
                    "final_answer": "BGP neighbor is down.",
                    "error_message": null
                  },
                  "metrics": {
                    "duration_seconds": 1.25,
                    "prompt_tokens": 100,
                    "completion_tokens": 40,
                    "total_tokens": 140,
                    "tool_calls": 2,
                    "mutating_tool_calls": 0,
                    "failed_tool_calls": 1
                  },
                  "artifacts": {
                    "run_log_path": "../netagent-benchmark/experiments/runs/20260626T010000Z-api-import/run.json",
                    "generated_code_path": null,
                    "verifier_output_path": null
                  }
                }
                """.formatted(experimentId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.run.experimentId").value(experimentId))
                .andExpect(jsonPath("$.run.source").value("FYP_AGENT_SERVICE"))
                .andExpect(jsonPath("$.run.status").value("COMPLETED"))
                .andExpect(jsonPath("$.run.fypRunId").value("20260626T010000Z-api-import"))
                .andExpect(jsonPath("$.result.score").value(0.82))
                .andExpect(jsonPath("$.metric.latencyMs").value(1250))
                .andExpect(jsonPath("$.metric.totalTokens").value(140))
                .andExpect(jsonPath("$.metric.failedToolCallCount").value(1));
    }

    @Test
    void validationErrorsReturnStructuredBody() throws Exception {
        postJson("/api/v1/experiments", """
                {
                  "name": "",
                  "domain": "NETWORK_BENCHMARK"
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details").isArray());
    }
}
