package com.frankliu.agentworkbench.api.dto;

public final class BenchmarkCaseDtos {

    private BenchmarkCaseDtos() {
    }

    public record Response(String caseId, String title, String schemaVersion) {
    }
}
