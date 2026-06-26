package com.frankliu.agentworkbench.api.dto;

import com.frankliu.agentworkbench.domain.AgentProvider;
import com.frankliu.agentworkbench.domain.ReasoningMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AgentConfigDtos {

    private AgentConfigDtos() {
    }

    public record Request(
            @NotBlank @Size(max = 160) String name,
            @NotNull AgentProvider provider,
            @NotBlank @Size(max = 120) String modelName,
            @NotBlank @Size(max = 120) String promptVersion,
            @NotBlank @Size(max = 80) String toolExposure,
            @NotNull @Min(1) @Max(100) Integer maxSteps,
            ReasoningMode reasoningMode
    ) {
    }

    public record Response(
            Long id,
            String name,
            AgentProvider provider,
            String modelName,
            String promptVersion,
            String toolExposure,
            Integer maxSteps,
            ReasoningMode reasoningMode,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
