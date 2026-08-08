package com.frankliu.agentworkbench.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class RunMetricDtos {

    private RunMetricDtos() {
    }

    public record Request(
            @NotNull Long runId,
            @Min(0) Long latencyMs,
            @Min(0) Integer agentStepCount,
            @Min(0) Integer promptTokens,
            @Min(0) Integer completionTokens,
            @Min(0) Integer totalTokens,
            @Min(0) Integer toolCallCount,
            @Min(0) Integer mutatingToolCallCount,
            @Min(0) Integer failedToolCallCount,
            @Min(0) Integer duplicateToolCallCount
    ) {
    }

    public record Response(
            Long id,
            Long runId,
            Long latencyMs,
            Integer agentStepCount,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            Integer toolCallCount,
            Integer mutatingToolCallCount,
            Integer failedToolCallCount,
            Integer duplicateToolCallCount,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
