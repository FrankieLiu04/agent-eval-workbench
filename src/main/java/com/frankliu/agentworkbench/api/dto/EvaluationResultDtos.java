package com.frankliu.agentworkbench.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class EvaluationResultDtos {

    private EvaluationResultDtos() {
    }

    public record Request(
            @NotNull Long runId,
            @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal score,
            @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal accuracy,
            @Min(0) Integer successCount,
            @Min(0) Integer failCount,
            @Min(0) Integer wrongCount,
            @Min(0) Integer formatErrorCount,
            @Min(0) Integer syntaxErrorCount,
            @Min(0) Integer testErrorCount,
            @Size(max = 2000) String summary
    ) {
    }

    public record Response(
            Long id,
            Long runId,
            BigDecimal score,
            BigDecimal accuracy,
            Integer successCount,
            Integer failCount,
            Integer wrongCount,
            Integer formatErrorCount,
            Integer syntaxErrorCount,
            Integer testErrorCount,
            String summary,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
