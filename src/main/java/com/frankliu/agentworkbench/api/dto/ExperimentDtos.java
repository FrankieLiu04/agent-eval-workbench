package com.frankliu.agentworkbench.api.dto;

import com.frankliu.agentworkbench.domain.ExperimentDomain;
import com.frankliu.agentworkbench.domain.ExperimentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ExperimentDtos {

    private ExperimentDtos() {
    }

    public record Request(
            @NotBlank @Size(max = 160) String name,
            @NotNull ExperimentDomain domain,
            @Size(max = 2000) String description,
            @Size(max = 160) String datasetName,
            @Size(max = 120) String baselineModel,
            ExperimentStatus status
    ) {
    }

    public record Response(
            Long id,
            String name,
            ExperimentDomain domain,
            String description,
            String datasetName,
            String baselineModel,
            ExperimentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
