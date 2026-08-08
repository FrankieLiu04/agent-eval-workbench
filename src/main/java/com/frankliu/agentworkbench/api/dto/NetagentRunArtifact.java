package com.frankliu.agentworkbench.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record NetagentRunArtifact(
        @JsonProperty("schema_version")
        @NotBlank @Pattern(regexp = "1\\.(1|2)", message = "must be 1.1 or 1.2") String schemaVersion,
        @JsonProperty("run_id")
        @NotBlank @Size(max = 120)
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "contains unsupported characters") String runId,
        @NotNull Instant timestamp,
        @Valid @NotNull Task task,
        @Valid @NotNull Agent agent,
        @Valid @NotNull Result result,
        @Valid @NotNull Metrics metrics,
        @NotNull JsonNode trace,
        @Valid Evaluation evaluation,
        JsonNode artifacts
) {
    public record Task(
            @NotBlank @Size(max = 2000) String description,
            @JsonProperty("case_id") @Size(max = 200) String caseId,
            @NotBlank @Size(max = 80) String mode
    ) {
    }

    public record Agent(
            @NotBlank @Size(max = 80) String provider,
            @NotBlank @Size(max = 160) String model,
            @JsonProperty("reasoning_mode") @Size(max = 40) String reasoningMode
    ) {
    }

    public record Result(
            @NotBlank
            @Pattern(regexp = "completed|failed", message = "must be completed or failed") String status,
            @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal score,
            @JsonProperty("final_answer") String finalAnswer,
            @JsonProperty("error_message") @Size(max = 2000) String errorMessage
    ) {
    }

    public record Metrics(
            @JsonProperty("agent_steps") @Min(0) Integer agentSteps,
            @JsonProperty("duration_seconds") @NotNull @DecimalMin("0.0") @DecimalMax("86400") BigDecimal durationSeconds,
            @JsonProperty("prompt_tokens") @Min(0) Integer promptTokens,
            @JsonProperty("completion_tokens") @Min(0) Integer completionTokens,
            @JsonProperty("total_tokens") @Min(0) Integer totalTokens,
            @JsonProperty("tool_calls") @NotNull @Min(0) Integer toolCalls,
            @JsonProperty("mutating_tool_calls") @NotNull @Min(0) Integer mutatingToolCalls,
            @JsonProperty("failed_tool_calls") @NotNull @Min(0) Integer failedToolCalls,
            @JsonProperty("duplicate_tool_calls") @Min(0) Integer duplicateToolCalls
    ) {
    }

    public record Evaluation(
            @NotBlank @Size(max = 80) String evaluator,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal score,
            @NotNull Boolean passed,
            @NotNull JsonNode checks
    ) {
    }
}
