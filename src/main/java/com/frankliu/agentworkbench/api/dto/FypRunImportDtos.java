package com.frankliu.agentworkbench.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.frankliu.agentworkbench.domain.RunStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class FypRunImportDtos {

    private FypRunImportDtos() {
    }

    public record Request(
            @NotNull Long experimentId,
            Long agentConfigId,
            @JsonProperty("workbench_import")
            @Valid @NotNull WorkbenchImport workbenchImport,
            @Valid @NotNull Benchmark benchmark,
            @Valid @NotNull Agent agent,
            @Valid @NotNull Result result,
            @Valid Metrics metrics,
            @Valid Artifacts artifacts
    ) {
    }

    public record WorkbenchImport(
            @JsonProperty("schema_version")
            @NotBlank @Size(max = 40) String schemaVersion,
            @JsonProperty("run_id")
            @NotBlank @Size(max = 120) String runId,
            Instant timestamp,
            @NotBlank @Size(max = 2000) String task
    ) {
    }

    public record Benchmark(
            @NotBlank @Size(max = 120) String name,
            @JsonProperty("case_id")
            @Size(max = 120) String caseId,
            @Size(max = 80) String level
    ) {
    }

    public record Agent(
            @Size(max = 80) String provider,
            @Size(max = 160) String model,
            @JsonProperty("reasoning_mode")
            @Size(max = 80) String reasoningMode
    ) {
    }

    public record Result(
            @NotBlank @Size(max = 40) String status,
            BigDecimal score,
            @JsonProperty("final_answer")
            @Size(max = 2000) String finalAnswer,
            @JsonProperty("error_message")
            @Size(max = 2000) String errorMessage
    ) {
    }

    public record Metrics(
            @JsonProperty("duration_seconds")
            BigDecimal durationSeconds,
            @JsonProperty("prompt_tokens")
            Integer promptTokens,
            @JsonProperty("completion_tokens")
            Integer completionTokens,
            @JsonProperty("total_tokens")
            Integer totalTokens,
            @JsonProperty("tool_calls")
            Integer toolCalls,
            @JsonProperty("mutating_tool_calls")
            Integer mutatingToolCalls,
            @JsonProperty("failed_tool_calls")
            Integer failedToolCalls
    ) {
    }

    public record Artifacts(
            @JsonProperty("run_log_path")
            @Size(max = 1000) String runLogPath,
            @JsonProperty("generated_code_path")
            @Size(max = 1000) String generatedCodePath,
            @JsonProperty("verifier_output_path")
            @Size(max = 1000) String verifierOutputPath
    ) {
    }

    public record Response(
            EvaluationRunDtos.Response run,
            EvaluationResultDtos.Response result,
            RunMetricDtos.Response metric
    ) {
    }

    public static RunStatus toRunStatus(String rawStatus) {
        return switch (rawStatus.trim().toLowerCase()) {
            case "completed", "success", "succeeded" -> RunStatus.COMPLETED;
            case "running", "in_progress" -> RunStatus.RUNNING;
            case "pending", "submitted" -> RunStatus.PENDING;
            case "cancelled", "canceled" -> RunStatus.CANCELLED;
            default -> RunStatus.FAILED;
        };
    }
}
