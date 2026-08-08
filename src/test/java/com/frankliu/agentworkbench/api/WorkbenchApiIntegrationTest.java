package com.frankliu.agentworkbench.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.frankliu.agentworkbench.domain.BenchmarkJob;
import com.frankliu.agentworkbench.repository.BenchmarkJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkbenchApiIntegrationTest {

    private static final Path TEST_ARTIFACT_ROOT = createArtifactRoot();
    private static final ObjectMapper JSON = new ObjectMapper();

    @DynamicPropertySource
    static void artifactRoot(DynamicPropertyRegistry registry) {
        registry.add("app.artifacts.root", TEST_ARTIFACT_ROOT::toString);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BenchmarkJobRepository jobRepository;

    @Value("${app.artifacts.root}")
    private Path artifactRoot;

    private ResultActions postJson(String path, String payload) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload));
    }

    private ResultActions putJson(String path, String payload) throws Exception {
        return mockMvc.perform(put(path)
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

    private long createLocalMockConfig(String name) throws Exception {
        String response = postJson("/api/v1/agent-configs", """
                {
                  "name": "%s",
                  "provider": "LOCAL_MOCK",
                  "modelName": "case-reference",
                  "promptVersion": "deterministic_v1",
                  "toolExposure": "replay",
                  "maxSteps": 7,
                  "reasoningMode": "DEFAULT"
                }
                """.formatted(name))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonTestSupport.extractLong(response, "id");
    }

    private long createDeepSeekConfig(String name, String model, String reasoningMode) throws Exception {
        String response = postJson("/api/v1/agent-configs", """
                {
                  "name": "%s",
                  "provider": "DEEPSEEK",
                  "modelName": "%s",
                  "promptVersion": "comparison_v1",
                  "toolExposure": "case_allowlist",
                  "maxSteps": 20,
                  "reasoningMode": "%s"
                }
                """.formatted(name, model, reasoningMode))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonTestSupport.extractLong(response, "id");
    }

    private long launchJob(long experimentId, long configId) throws Exception {
        String response = postJson("/api/v1/benchmark-jobs", """
                {
                  "experimentId": %d,
                  "caseId": "replay/ospf-adjacency-down-v1",
                  "agentConfigId": %d
                }
                """.formatted(experimentId, configId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.provider").value("LOCAL_MOCK"))
                .andExpect(jsonPath("$.model").value("case-reference"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonTestSupport.extractLong(response, "id");
    }

    private String claimJob(String workerId) throws Exception {
        return postJson("/api/v1/benchmark-jobs/claim", """
                {"workerId": "%s"}
                """.formatted(workerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("scripted"))
                .andExpect(jsonPath("$.model").value("case-reference"))
                .andExpect(jsonPath("$.maxTurns").value(7))
                .andExpect(jsonPath("$.timeoutSeconds").value(300))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String claimDeepSeekJob(String workerId, String reasoningMode) throws Exception {
        return postJson("/api/v1/benchmark-jobs/claim", """
                {"workerId": "%s"}
                """.formatted(workerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("deepseek"))
                .andExpect(jsonPath("$.reasoningMode").value(reasoningMode))
                .andExpect(jsonPath("$.maxTurns").value(20))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String comparisonArtifact(
            String runId,
            String model,
            String reasoningMode,
            boolean passed,
            double score,
            double durationSeconds,
            int totalTokens,
            int toolCalls,
            int failedToolCalls
    ) throws IOException {
        String fixture = new ClassPathResource("fixtures/netagent-run-schema-1.1.json")
                .getContentAsString(StandardCharsets.UTF_8);
        ObjectNode artifact = (ObjectNode) JSON.readTree(fixture);
        artifact.put("run_id", runId);
        ((ObjectNode) artifact.get("agent")).put("provider", "deepseek")
                .put("model", model).put("reasoning_mode", reasoningMode);
        ((ObjectNode) artifact.get("result")).put("score", score);
        ((ObjectNode) artifact.get("evaluation")).put("score", score).put("passed", passed);
        ((ObjectNode) artifact.get("metrics")).put("duration_seconds", durationSeconds)
                .put("prompt_tokens", totalTokens).put("completion_tokens", 0)
                .put("total_tokens", totalTokens).put("tool_calls", toolCalls)
                .put("failed_tool_calls", failedToolCalls);
        return JSON.writeValueAsString(artifact);
    }

    private void completeJob(String claim, String workerId, String artifact) throws Exception {
        completeJob(claim, workerId, artifact, "SUCCEEDED");
    }

    private void completeJob(String claim, String workerId, String artifact, String expectedStatus) throws Exception {
        long jobId = JsonTestSupport.extractLong(claim, "jobId");
        mockMvc.perform(post("/api/v1/benchmark-jobs/{id}/complete", jobId)
                        .header("X-Netagent-Worker-Id", workerId)
                        .header("X-Netagent-Claim-Token", JsonTestSupport.extractString(claim, "claimToken"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(artifact))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expectedStatus));
    }

    private static Path createArtifactRoot() {
        try {
            return Files.createTempDirectory("workbench-artifacts-");
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Test
    void seedDataIsQueryable() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Netagent Eval Workbench")))
                .andExpect(content().string(containsString("id=\"root\"")))
                .andExpect(content().string(containsString("type=\"module\"")));

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
    void unsupportedNetagentSchemaIsRejected() throws Exception {
        long experimentId = createExperiment("Unsupported schema benchmark");
        String artifact = new ClassPathResource("fixtures/netagent-run-schema-1.1.json")
                .getContentAsString(StandardCharsets.UTF_8)
                .replace("\"schema_version\": \"1.1\"", "\"schema_version\": \"1.0\"");

        postJson("/api/v1/imports/netagent-run-json?experimentId=" + experimentId, artifact)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid Netagent artifact fields: schemaVersion"));
    }

    @Test
    void completedBenchmarkArtifactRequiresConsistentEvaluationEvidence() throws Exception {
        long experimentId = createExperiment("Invalid benchmark evidence");
        ObjectNode missingEvaluation = (ObjectNode) JSON.readTree(new ClassPathResource(
                "fixtures/netagent-run-schema-1.1.json").getContentAsString(StandardCharsets.UTF_8));
        missingEvaluation.remove("evaluation");

        postJson("/api/v1/imports/netagent-run-json?experimentId=" + experimentId,
                JSON.writeValueAsString(missingEvaluation))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Completed benchmark artifacts require matching evaluation evidence"));

        ObjectNode inconsistentTokens = missingEvaluation.deepCopy();
        inconsistentTokens.set("evaluation", JSON.readTree("""
                {"evaluator":"evidence","score":1,"passed":true,"checks":[]}
                """));
        ((ObjectNode) inconsistentTokens.get("metrics")).put("total_tokens", 379);
        postJson("/api/v1/imports/netagent-run-json?experimentId=" + experimentId,
                JSON.writeValueAsString(inconsistentTokens))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid Netagent artifact token totals"));
    }

    @Test
    void failedNetagentRunMayHaveNullTokenMetrics() throws Exception {
        long experimentId = createExperiment("Failed run benchmark");
        String artifact = new ClassPathResource("fixtures/netagent-run-schema-1.1.json")
                .getContentAsString(StandardCharsets.UTF_8)
                .replace("20260726T071207Z-a7c15e98", "20260726T071207Z-failed01")
                .replace("\"status\": \"completed\"", "\"status\": \"failed\"")
                .replace("\"prompt_tokens\": 300", "\"prompt_tokens\": null")
                .replace("\"completion_tokens\": 80", "\"completion_tokens\": null")
                .replace("\"total_tokens\": 380", "\"total_tokens\": null");

        postJson("/api/v1/imports/netagent-run-json?experimentId=" + experimentId, artifact)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.run.status").value("FAILED"))
                .andExpect(jsonPath("$.totalTokens").doesNotExist());
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
    void importNetagentRunJsonArchivesAndExposesTheRun() throws Exception {
        long experimentId = createExperiment("FYP import benchmark");
        String artifact = new ClassPathResource("fixtures/netagent-run-schema-1.1.json")
                .getContentAsString(StandardCharsets.UTF_8);
        artifact = artifact.substring(0, artifact.lastIndexOf('}'))
                + ",\n  \"producer_extension\": {\"preserved\": true}\n}";

        String importResponse = postJson("/api/v1/imports/netagent-run-json?experimentId=" + experimentId, artifact)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.run.experimentId").value(experimentId))
                .andExpect(jsonPath("$.run.source").value("NETAGENT_BENCHMARK_IMPORT"))
                .andExpect(jsonPath("$.run.status").value("COMPLETED"))
                .andExpect(jsonPath("$.run.runId").value("20260726T071207Z-a7c15e98"))
                .andExpect(jsonPath("$.run.caseId").value("replay/ospf-adjacency-down-v1"))
                .andExpect(jsonPath("$.run.agentModel").value("case-reference"))
                .andExpect(jsonPath("$.score").value(1.0))
                .andExpect(jsonPath("$.durationMs").value(2))
                .andExpect(jsonPath("$.totalTokens").value(380))
                .andExpect(jsonPath("$.agentSteps").doesNotExist())
                .andExpect(jsonPath("$.duplicateToolCalls").doesNotExist())
                .andExpect(jsonPath("$.artifact.trace.steps[0].tool_calls[0].name")
                        .value("show_ip_ospf_neighbor"))
                .andExpect(jsonPath("$.artifact.producer_extension.preserved").value(true))
                .andReturn().getResponse().getContentAsString();

        long importedRunId = JSON.readTree(importResponse).at("/run/id").asLong();
        putJson("/api/v1/evaluation-runs/" + importedRunId, """
                {
                  "experimentId": %d,
                  "source": "MANUAL",
                  "task": "Attempt to rewrite imported evidence",
                  "status": "COMPLETED"
                }
                """.formatted(experimentId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Imported benchmark runs are immutable"));

        String importedResult = mockMvc.perform(get("/api/v1/evaluation-results")
                        .param("runId", Long.toString(importedRunId)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        putJson("/api/v1/evaluation-results/" + JsonTestSupport.extractLong(importedResult, "id"), """
                {"runId": %d, "score": 0.5, "passed": false}
                """.formatted(importedRunId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Imported benchmark results are immutable"));

        String importedMetric = mockMvc.perform(get("/api/v1/run-metrics")
                        .param("runId", Long.toString(importedRunId)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        putJson("/api/v1/run-metrics/" + JsonTestSupport.extractLong(importedMetric, "id"), """
                {"runId": %d, "latencyMs": 999}
                """.formatted(importedRunId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Imported benchmark metrics are immutable"));

        Path storedArtifact = artifactRoot.resolve("20260726T071207Z-a7c15e98/run.json");
        org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(storedArtifact)).isTrue();

        mockMvc.perform(get("/api/v1/evaluation-runs")
                        .param("experimentId", Long.toString(experimentId))
                        .param("model", "case-reference"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].run.caseId").value("replay/ospf-adjacency-down-v1"))
                .andExpect(jsonPath("$[0].score").value(1.0));

        postJson("/api/v1/imports/netagent-run-json?experimentId=" + experimentId, artifact)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Netagent run already imported: 20260726T071207Z-a7c15e98"));
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

    @Test
    void benchmarkJobLaunchClaimAndCompleteImportsAndLinksRun() throws Exception {
        long experimentId = createExperiment("Queued completion benchmark");
        long configId = createLocalMockConfig("Queued completion local mock");
        long jobId = launchJob(experimentId, configId);

        putJson("/api/v1/agent-configs/" + configId, """
                {
                  "name": "Edited after queueing",
                  "provider": "OTHER",
                  "modelName": "changed-model",
                  "promptVersion": "changed",
                  "toolExposure": "none",
                  "maxSteps": 1,
                  "reasoningMode": "DEFAULT"
                }
                """)
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/benchmark-cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].caseId").value("replay/ospf-adjacency-down-v1"))
                .andExpect(jsonPath("$[0].schemaVersion").value("1.0"));

        String workerId = "worker-complete";
        String claim = claimJob(workerId);
        String claimToken = JsonTestSupport.extractString(claim, "claimToken");
        org.assertj.core.api.Assertions.assertThat(JsonTestSupport.extractLong(claim, "jobId")).isEqualTo(jobId);
        org.assertj.core.api.Assertions.assertThat(JsonTestSupport.extractString(claim, "reasoningMode"))
                .isEqualTo("DISABLED");

        String runId = "queue-" + UUID.randomUUID();
        String artifact = new ClassPathResource("fixtures/netagent-run-schema-1.1.json")
                .getContentAsString(StandardCharsets.UTF_8)
                .replace("20260726T071207Z-a7c15e98", runId);

        mockMvc.perform(post("/api/v1/benchmark-jobs/{id}/complete", jobId)
                        .header("X-Netagent-Worker-Id", workerId)
                        .header("X-Netagent-Claim-Token", claimToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(artifact.replace("\"mode\": \"case-run\"", "\"mode\": \"agent-run\"")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Netagent artifact does not match the claimed benchmark selection"));

        mockMvc.perform(post("/api/v1/benchmark-jobs/{id}/complete", jobId)
                        .header("X-Netagent-Worker-Id", workerId)
                        .header("X-Netagent-Claim-Token", claimToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(artifact))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.agentConfigName").value("Queued completion local mock"))
                .andExpect(jsonPath("$.reasoningMode").value("DISABLED"))
                .andExpect(jsonPath("$.evaluationRunId").isNumber())
                .andExpect(jsonPath("$.finishedAt").exists());

        mockMvc.perform(get("/api/v1/evaluation-runs")
                        .param("experimentId", Long.toString(experimentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].run.runId").value(runId));
    }

    @Test
    void benchmarkBatchPreservesSnapshotsAndAggregatesProfileMetrics() throws Exception {
        long experimentId = createExperiment("DeepSeek comparison benchmark");
        long highConfigId = createDeepSeekConfig(
                "DeepSeek V4 Flash / High test", "deepseek-v4-flash", "HIGH");
        long maxConfigId = createDeepSeekConfig(
                "DeepSeek V4 Pro / Max test", "deepseek-v4-pro", "MAX");

        String batch = postJson("/api/v1/benchmark-jobs/batches", """
                {
                  "experimentId": %d,
                  "caseId": "replay/ospf-adjacency-down-v1",
                  "agentConfigIds": [%d, %d],
                  "repetitions": 2
                }
                """.formatted(experimentId, highConfigId, maxConfigId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobCount").value(4))
                .andExpect(jsonPath("$.jobs", hasSize(4)))
                .andExpect(jsonPath("$.jobs[0].reasoningMode").value("HIGH"))
                .andExpect(jsonPath("$.jobs[2].reasoningMode").value("MAX"))
                .andReturn().getResponse().getContentAsString();
        String batchId = JsonTestSupport.extractString(batch, "batchId");

        putJson("/api/v1/agent-configs/" + highConfigId, """
                {
                  "name": "Edited after batch launch",
                  "provider": "OTHER",
                  "modelName": "changed-model",
                  "promptVersion": "changed",
                  "toolExposure": "none",
                  "maxSteps": 1,
                  "reasoningMode": "DEFAULT"
                }
                """).andExpect(status().isOk());

        String highOne = claimDeepSeekJob("batch-worker-1", "HIGH");
        completeJob(highOne, "batch-worker-1", comparisonArtifact(
                "batch-high-1-" + UUID.randomUUID(), "deepseek-v4-flash", "HIGH",
                true, 1.0, 1.0, 100, 2, 0));
        String highTwo = claimDeepSeekJob("batch-worker-2", "HIGH");
        completeJob(highTwo, "batch-worker-2", comparisonArtifact(
                "batch-high-2-" + UUID.randomUUID(), "deepseek-v4-flash", "HIGH",
                false, 0.5, 3.0, 300, 4, 1));
        String maxOne = claimDeepSeekJob("batch-worker-3", "MAX");
        completeJob(maxOne, "batch-worker-3", comparisonArtifact(
                "batch-max-1-" + UUID.randomUUID(), "deepseek-v4-pro", "MAX",
                true, 1.0, 2.0, 200, 3, 0));
        String maxTwo = claimDeepSeekJob("batch-worker-4", "MAX");
        ObjectNode failedArtifact = (ObjectNode) JSON.readTree(comparisonArtifact(
                "batch-max-2-" + UUID.randomUUID(), "deepseek-v4-pro", "MAX",
                false, 0.0, 9.0, 900, 9, 2));
        ((ObjectNode) failedArtifact.get("result")).put("status", "failed")
                .put("error_message", "provider execution failed");
        completeJob(maxTwo, "batch-worker-4", JSON.writeValueAsString(failedArtifact), "FAILED");

        mockMvc.perform(get("/api/v1/benchmark-jobs/batches/{batchId}/comparison", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(0))
                .andExpect(jsonPath("$.terminal").value(4))
                .andExpect(jsonPath("$.profiles", hasSize(2)))
                .andExpect(jsonPath("$.profiles[0].agentConfigName")
                        .value("DeepSeek V4 Flash / High test"))
                .andExpect(jsonPath("$.profiles[0].reasoningMode").value("HIGH"))
                .andExpect(jsonPath("$.profiles[0].evaluated").value(2))
                .andExpect(jsonPath("$.profiles[0].passed").value(1))
                .andExpect(jsonPath("$.profiles[0].passRate").value(0.5))
                .andExpect(jsonPath("$.profiles[0].averageScore").value(0.75))
                .andExpect(jsonPath("$.profiles[0].averageLatencyMs").value(2000.0))
                .andExpect(jsonPath("$.profiles[0].averageTotalTokens").value(200.0))
                .andExpect(jsonPath("$.profiles[0].averageToolCalls").value(3.0))
                .andExpect(jsonPath("$.profiles[0].toolSuccessRate").value(0.8333))
                .andExpect(jsonPath("$.profiles[1].evaluated").value(1))
                .andExpect(jsonPath("$.profiles[1].passed").value(1))
                .andExpect(jsonPath("$.profiles[1].executionFailures").value(1))
                .andExpect(jsonPath("$.profiles[1].passRate").value(1.0))
                .andExpect(jsonPath("$.profiles[1].averageLatencyMs").value(2000.0))
                .andExpect(jsonPath("$.profiles[1].averageTotalTokens").value(200.0));

        postJson("/api/v1/benchmark-jobs/batches", """
                {
                  "experimentId": %d,
                  "caseId": "replay/ospf-adjacency-down-v1",
                  "agentConfigIds": [%d, %d],
                  "repetitions": 1
                }
                """.formatted(experimentId, maxConfigId, maxConfigId))
                .andExpect(status().isConflict());
    }

    @Test
    void benchmarkJobsSupportQueuedAndRunningCancellation() throws Exception {
        long experimentId = createExperiment("Queue cancellation benchmark");
        long configId = createLocalMockConfig("Queue cancellation local mock");

        long queuedJobId = launchJob(experimentId, configId);
        mockMvc.perform(post("/api/v1/benchmark-jobs/{id}/cancel", queuedJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        long runningJobId = launchJob(experimentId, configId);
        String workerId = "worker-cancel";
        String claim = claimJob(workerId);
        String claimToken = JsonTestSupport.extractString(claim, "claimToken");
        org.assertj.core.api.Assertions.assertThat(JsonTestSupport.extractLong(claim, "jobId"))
                .isEqualTo(runningJobId);

        mockMvc.perform(post("/api/v1/benchmark-jobs/{id}/cancel", runningJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCEL_REQUESTED"));
        mockMvc.perform(post("/api/v1/benchmark-jobs/{id}/heartbeat", runningJobId)
                        .header("X-Netagent-Worker-Id", workerId)
                        .header("X-Netagent-Claim-Token", claimToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCEL_REQUESTED"))
                .andExpect(jsonPath("$.cancellationRequested").value(true));

        String cancelledArtifact = new ClassPathResource("fixtures/netagent-run-schema-1.1.json")
                .getContentAsString(StandardCharsets.UTF_8)
                .replace("20260726T071207Z-a7c15e98", "cancelled-" + UUID.randomUUID());
        mockMvc.perform(post("/api/v1/benchmark-jobs/{id}/complete", runningJobId)
                        .header("X-Netagent-Worker-Id", workerId)
                        .header("X-Netagent-Claim-Token", claimToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelledArtifact))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.evaluationRunId").doesNotExist());
        mockMvc.perform(post("/api/v1/benchmark-jobs/{id}/cancelled", runningJobId)
                        .header("X-Netagent-Worker-Id", workerId)
                        .header("X-Netagent-Claim-Token", claimToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void benchmarkWorkerMayReportFailureOrTimeout() throws Exception {
        long experimentId = createExperiment("Queue failure benchmark");
        long configId = createLocalMockConfig("Queue failure local mock");

        long failedJobId = launchJob(experimentId, configId);
        String failedClaim = claimJob("worker-failed");
        mockMvc.perform(post("/api/v1/benchmark-jobs/{id}/fail", failedJobId)
                        .header("X-Netagent-Worker-Id", "worker-failed")
                        .header("X-Netagent-Claim-Token", JsonTestSupport.extractString(failedClaim, "claimToken"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"worker process failed\",\"timedOut\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("worker process failed"));

        long timedOutJobId = launchJob(experimentId, configId);
        String timedOutClaim = claimJob("worker-timeout");
        mockMvc.perform(post("/api/v1/benchmark-jobs/{id}/fail", timedOutJobId)
                        .header("X-Netagent-Worker-Id", "worker-timeout")
                        .header("X-Netagent-Claim-Token", JsonTestSupport.extractString(timedOutClaim, "claimToken"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"worker deadline exceeded\",\"timedOut\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TIMED_OUT"))
                .andExpect(jsonPath("$.finishedAt").exists());
    }

    @Test
    void serverDeadlineAndCancellationTransitionsAreAuthoritative() throws Exception {
        long experimentId = createExperiment("Queue deadline benchmark");
        long configId = createLocalMockConfig("Queue deadline local mock");

        long expiredJobId = launchJob(experimentId, configId);
        String expiredClaim = claimJob("worker-expired");
        expireJob(expiredJobId);
        mockMvc.perform(post("/api/v1/benchmark-jobs/{id}/heartbeat", expiredJobId)
                        .header("X-Netagent-Worker-Id", "worker-expired")
                        .header("X-Netagent-Claim-Token", JsonTestSupport.extractString(expiredClaim, "claimToken")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TIMED_OUT"));

        long cancelledJobId = launchJob(experimentId, configId);
        String cancelledClaim = claimJob("worker-deadline-cancel");
        String cancelledToken = JsonTestSupport.extractString(cancelledClaim, "claimToken");
        mockMvc.perform(post("/api/v1/benchmark-jobs/{id}/cancelled", cancelledJobId)
                        .header("X-Netagent-Worker-Id", "worker-deadline-cancel")
                        .header("X-Netagent-Claim-Token", cancelledToken))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/benchmark-jobs/{id}/cancel", cancelledJobId))
                .andExpect(status().isOk());
        expireJob(cancelledJobId);
        mockMvc.perform(post("/api/v1/benchmark-jobs/{id}/heartbeat", cancelledJobId)
                        .header("X-Netagent-Worker-Id", "worker-deadline-cancel")
                        .header("X-Netagent-Claim-Token", cancelledToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCEL_REQUESTED"))
                .andExpect(jsonPath("$.cancellationRequested").value(true));
    }

    private void expireJob(long jobId) {
        BenchmarkJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStartedAt(Instant.now().minusSeconds(job.getTimeoutSeconds() + 1L));
        jobRepository.saveAndFlush(job);
    }
}
