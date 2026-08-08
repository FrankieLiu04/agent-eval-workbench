package com.frankliu.agentworkbench.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NetagentSchema12IntegrationTest {

    private static final Path ARTIFACT_ROOT = createArtifactRoot();

    @DynamicPropertySource
    static void artifactRoot(DynamicPropertyRegistry registry) {
        registry.add("app.artifacts.root", ARTIFACT_ROOT::toString);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void importsSchema12TrajectoryEfficiencyMetrics() throws Exception {
        String experiment = mockMvc.perform(post("/api/v1/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Schema 1.2 import %s",
                                  "domain": "NETWORK_BENCHMARK",
                                  "status": "ACTIVE"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long experimentId = objectMapper.readTree(experiment).get("id").asLong();

        String artifact = new ClassPathResource("fixtures/netagent-run-schema-1.2.json")
                .getContentAsString(StandardCharsets.UTF_8)
                .replace("20260807T010000Z-schema12", "schema12-" + UUID.randomUUID());

        mockMvc.perform(post("/api/v1/imports/netagent-run-json")
                        .param("experimentId", Long.toString(experimentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(artifact))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.run.schemaVersion").value("1.2"))
                .andExpect(jsonPath("$.agentSteps").value(3))
                .andExpect(jsonPath("$.duplicateToolCalls").value(1))
                .andExpect(jsonPath("$.artifact.metrics.agent_steps").value(3))
                .andExpect(jsonPath("$.artifact.metrics.duplicate_tool_calls").value(1))
                .andExpect(jsonPath("$.artifact.trace.steps.length()").value(3))
                .andExpect(jsonPath("$.artifact.evaluation.evaluator")
                        .value("campus_repair_contract_v1"))
                .andExpect(jsonPath("$.artifact.evaluation.diagnostic_score").value(1.0))
                .andExpect(jsonPath("$.artifact.evaluation.checks.length()").value(5));
    }

    private static Path createArtifactRoot() {
        try {
            return Files.createTempDirectory("workbench-schema12-");
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
