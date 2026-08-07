package com.frankliu.agentworkbench.service;

import com.frankliu.agentworkbench.api.dto.EvaluationRunDtos;
import com.frankliu.agentworkbench.api.dto.NetagentRunArtifact;
import com.frankliu.agentworkbench.api.error.ConflictException;
import com.frankliu.agentworkbench.domain.AgentConfig;
import com.frankliu.agentworkbench.domain.EvaluationResult;
import com.frankliu.agentworkbench.domain.EvaluationRun;
import com.frankliu.agentworkbench.domain.Experiment;
import com.frankliu.agentworkbench.domain.RunMetric;
import com.frankliu.agentworkbench.domain.RunSource;
import com.frankliu.agentworkbench.domain.RunStatus;
import com.frankliu.agentworkbench.repository.EvaluationResultRepository;
import com.frankliu.agentworkbench.repository.EvaluationRunRepository;
import com.frankliu.agentworkbench.repository.RunMetricRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
public class NetagentRunImportService {

    private final EvaluationRunRepository runRepository;
    private final EvaluationResultRepository resultRepository;
    private final RunMetricRepository metricRepository;
    private final ExperimentService experimentService;
    private final AgentConfigService agentConfigService;
    private final RunArtifactStorage artifactStorage;
    private final EvaluationRunService runService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public NetagentRunImportService(
            EvaluationRunRepository runRepository,
            EvaluationResultRepository resultRepository,
            RunMetricRepository metricRepository,
            ExperimentService experimentService,
            AgentConfigService agentConfigService,
            RunArtifactStorage artifactStorage,
            EvaluationRunService runService,
            ObjectMapper objectMapper,
            Validator validator
    ) {
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.metricRepository = metricRepository;
        this.experimentService = experimentService;
        this.agentConfigService = agentConfigService;
        this.artifactStorage = artifactStorage;
        this.runService = runService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public EvaluationRunDtos.DetailResponse importRun(
            Long experimentId,
            Long agentConfigId,
            JsonNode artifactJson
    ) {
        NetagentRunArtifact artifact = inspect(artifactJson);
        EvaluationRun run = importArtifact(experimentId, agentConfigId, artifact, artifactJson);
        return runService.getDetail(run.getId());
    }

    public EvaluationRun importArtifact(
            Long experimentId,
            Long agentConfigId,
            NetagentRunArtifact artifact,
            JsonNode artifactJson
    ) {
        if (runRepository.existsByFypRunId(artifact.runId())) {
            throw duplicateRun(artifact.runId());
        }

        EvaluationRun run = createRun(experimentId, agentConfigId, artifact);
        try {
            runRepository.saveAndFlush(run);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateRun(artifact.runId());
        }

        resultRepository.save(createResult(run, artifact));
        metricRepository.save(createMetric(run, artifact.metrics()));
        run.setArtifactPath(artifactStorage.store(artifact.runId(), artifactJson));
        return runRepository.save(run);
    }

    private EvaluationRun createRun(Long experimentId, Long agentConfigId, NetagentRunArtifact artifact) {
        Experiment experiment = experimentService.getEntity(experimentId);
        AgentConfig config = agentConfigId == null ? null : agentConfigService.getEntity(agentConfigId);
        EvaluationRun run = new EvaluationRun();
        run.setExperiment(experiment);
        run.setAgentConfig(config);
        run.setSource(RunSource.NETAGENT_BENCHMARK_IMPORT);
        run.setTask(artifact.task().description());
        run.setStatus("completed".equals(artifact.result().status()) ? RunStatus.COMPLETED : RunStatus.FAILED);
        run.setFypRunId(artifact.runId());
        run.setSchemaVersion(artifact.schemaVersion());
        run.setCaseId(artifact.task().caseId());
        run.setTaskMode(artifact.task().mode());
        run.setAgentProvider(artifact.agent().provider());
        run.setAgentModel(artifact.agent().model());
        run.setStartedAt(artifact.timestamp());
        run.setFinishedAt(artifact.timestamp().plusMillis(toMillis(artifact.metrics().durationSeconds())));
        run.setErrorMessage(artifact.result().errorMessage());
        return run;
    }

    private EvaluationResult createResult(EvaluationRun run, NetagentRunArtifact artifact) {
        EvaluationResult result = new EvaluationResult();
        result.setRun(run);
        result.setScore(artifact.result().score());
        result.setPassed(artifact.evaluation() == null ? null : artifact.evaluation().passed());
        result.setSummary(clip(artifact.result().finalAnswer()));
        return result;
    }

    private RunMetric createMetric(EvaluationRun run, NetagentRunArtifact.Metrics source) {
        RunMetric metric = new RunMetric();
        metric.setRun(run);
        metric.setLatencyMs(toMillis(source.durationSeconds()));
        metric.setAgentStepCount(source.agentSteps());
        metric.setPromptTokens(source.promptTokens());
        metric.setCompletionTokens(source.completionTokens());
        metric.setTotalTokens(source.totalTokens());
        metric.setToolCallCount(source.toolCalls());
        metric.setMutatingToolCallCount(source.mutatingToolCalls());
        metric.setFailedToolCallCount(source.failedToolCalls());
        metric.setDuplicateToolCallCount(source.duplicateToolCalls());
        return metric;
    }

    private long toMillis(BigDecimal seconds) {
        if (seconds == null) {
            return 0L;
        }
        return seconds.multiply(BigDecimal.valueOf(1000)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private String clip(String value) {
        return value == null || value.length() <= 2000 ? value : value.substring(0, 2000);
    }

    private ConflictException duplicateRun(String runId) {
        return new ConflictException("Netagent run already imported: " + runId);
    }

    public NetagentRunArtifact inspect(JsonNode artifactJson) {
        NetagentRunArtifact artifact = objectMapper.treeToValue(artifactJson, NetagentRunArtifact.class);
        String violations = validator.validate(artifact).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .sorted()
                .collect(Collectors.joining(", "));
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid Netagent artifact fields: " + violations);
        }
        validateBenchmarkEvidence(artifact);
        return artifact;
    }

    private void validateBenchmarkEvidence(NetagentRunArtifact artifact) {
        NetagentRunArtifact.Metrics metrics = artifact.metrics();
        if (metrics.failedToolCalls() > metrics.toolCalls()
                || metrics.mutatingToolCalls() > metrics.toolCalls()
                || (metrics.duplicateToolCalls() != null && metrics.duplicateToolCalls() > metrics.toolCalls())) {
            throw new IllegalArgumentException("Invalid Netagent artifact tool-call metrics");
        }
        if (metrics.promptTokens() != null && metrics.completionTokens() != null
                && metrics.totalTokens() != null
                && metrics.promptTokens() + metrics.completionTokens() != metrics.totalTokens()) {
            throw new IllegalArgumentException("Invalid Netagent artifact token totals");
        }
        if (!"case-run".equals(artifact.task().mode()) || !"completed".equals(artifact.result().status())) {
            return;
        }
        if (artifact.evaluation() == null || artifact.result().score() == null
                || !artifact.evaluation().checks().isArray()
                || artifact.result().score().compareTo(artifact.evaluation().score()) != 0) {
            throw new IllegalArgumentException("Completed benchmark artifacts require matching evaluation evidence");
        }
    }
}
