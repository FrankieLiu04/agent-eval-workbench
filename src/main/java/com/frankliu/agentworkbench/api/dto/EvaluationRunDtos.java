package com.frankliu.agentworkbench.api.dto;

import com.frankliu.agentworkbench.domain.RunSource;
import com.frankliu.agentworkbench.domain.RunStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import tools.jackson.databind.JsonNode;

public final class EvaluationRunDtos {

    private EvaluationRunDtos() {
    }

    public record Request(
            @NotNull Long experimentId,
            Long agentConfigId,
            @NotNull RunSource source,
            @NotBlank @Size(max = 2000) String task,
            RunStatus status,
            @Size(max = 120) String runId,
            @Size(max = 1000) String artifactPath,
            Integer exitCode,
            Instant startedAt,
            Instant finishedAt,
            @Size(max = 2000) String errorMessage
    ) {
    }

    public record Response(
            Long id,
            Long experimentId,
            String experimentName,
            Long agentConfigId,
            String agentConfigName,
            RunSource source,
            String task,
            RunStatus status,
            String runId,
            String schemaVersion,
            String caseId,
            String taskMode,
            String agentProvider,
            String agentModel,
            String artifactPath,
            Integer exitCode,
            Instant startedAt,
            Instant finishedAt,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record SummaryResponse(
            Response run,
            BigDecimal score,
            Long durationMs,
            Integer totalTokens,
            Integer toolCalls
    ) {
    }

    public record DetailResponse(
            Response run,
            BigDecimal score,
            Long durationMs,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            Integer toolCalls,
            Integer mutatingToolCalls,
            Integer failedToolCalls,
            JsonNode artifact
    ) {
    }
}
