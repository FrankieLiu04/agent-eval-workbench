package com.frankliu.agentworkbench.api.dto;

import com.frankliu.agentworkbench.domain.AgentProvider;
import com.frankliu.agentworkbench.domain.BenchmarkJobStatus;
import com.frankliu.agentworkbench.domain.ReasoningMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

public final class BenchmarkJobDtos {

    private BenchmarkJobDtos() {
    }

    public record LaunchRequest(
            @NotNull Long experimentId,
            @NotBlank String caseId,
            @NotNull Long agentConfigId
    ) {
    }

    public record BatchLaunchRequest(
            @NotNull Long experimentId,
            @NotBlank String caseId,
            @NotNull @Size(min = 1, max = 12) List<@NotNull Long> agentConfigIds,
            @Min(1) @Max(20) int repetitions
    ) {
    }

    public record ClaimRequest(@NotBlank @Size(max = 160) String workerId) {
    }

    public record FailureRequest(@NotBlank @Size(max = 2000) String message, boolean timedOut) {
    }

    public record JobResponse(
            Long id,
            BenchmarkJobStatus status,
            Long experimentId,
            String experimentName,
            String caseId,
            String caseTitle,
            String caseSchemaVersion,
            Long agentConfigId,
            String agentConfigName,
            AgentProvider provider,
            String model,
            String promptVersion,
            String toolExposure,
            ReasoningMode reasoningMode,
            String batchId,
            Integer repetition,
            Long evaluationRunId,
            String workerId,
            Instant createdAt,
            Instant startedAt,
            Instant heartbeatAt,
            Instant finishedAt,
            String errorMessage
    ) {
    }

    public record ClaimResponse(
            Long jobId,
            String claimToken,
            String caseId,
            String provider,
            String model,
            ReasoningMode reasoningMode,
            Integer maxTurns,
            Integer timeoutSeconds
    ) {
    }

    public record WorkerStatusResponse(BenchmarkJobStatus status, boolean cancellationRequested) {
    }

    public record BatchLaunchResponse(String batchId, int jobCount, List<JobResponse> jobs) {
    }

    public record BatchComparisonResponse(
            String batchId,
            Long experimentId,
            String caseId,
            String caseTitle,
            String caseSchemaVersion,
            int repetitions,
            int requested,
            int active,
            int terminal,
            List<ProfileComparison> profiles,
            List<JobResponse> jobs
    ) {
    }

    public record ProfileComparison(
            Long agentConfigId,
            String agentConfigName,
            AgentProvider provider,
            String model,
            String promptVersion,
            String toolExposure,
            ReasoningMode reasoningMode,
            int requested,
            int queued,
            int running,
            int evaluated,
            int passed,
            int executionFailures,
            int timedOut,
            int cancelled,
            BigDecimal passRate,
            BigDecimal averageScore,
            BigDecimal averageLatencyMs,
            BigDecimal averageTotalTokens,
            BigDecimal averageToolCalls,
            BigDecimal averageFailedToolCalls,
            BigDecimal toolSuccessRate
    ) {
    }
}
