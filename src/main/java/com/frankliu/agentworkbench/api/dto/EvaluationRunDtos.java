package com.frankliu.agentworkbench.api.dto;

import com.frankliu.agentworkbench.domain.RunSource;
import com.frankliu.agentworkbench.domain.RunStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class EvaluationRunDtos {

    private EvaluationRunDtos() {
    }

    public record Request(
            @NotNull Long experimentId,
            Long agentConfigId,
            @NotNull RunSource source,
            @NotBlank @Size(max = 2000) String task,
            RunStatus status,
            @Size(max = 120) String fypRunId,
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
            String fypRunId,
            String artifactPath,
            Integer exitCode,
            Instant startedAt,
            Instant finishedAt,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
