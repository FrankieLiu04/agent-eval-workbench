package com.frankliu.agentworkbench.service;

import com.frankliu.agentworkbench.api.dto.EvaluationRunDtos;
import com.frankliu.agentworkbench.api.error.ConflictException;
import com.frankliu.agentworkbench.api.error.NotFoundException;
import com.frankliu.agentworkbench.domain.AgentConfig;
import com.frankliu.agentworkbench.domain.EvaluationResult;
import com.frankliu.agentworkbench.domain.EvaluationRun;
import com.frankliu.agentworkbench.domain.Experiment;
import com.frankliu.agentworkbench.domain.RunMetric;
import com.frankliu.agentworkbench.domain.RunSource;
import com.frankliu.agentworkbench.domain.RunStatus;
import com.frankliu.agentworkbench.repository.EvaluationRunRepository;
import com.frankliu.agentworkbench.repository.EvaluationResultRepository;
import com.frankliu.agentworkbench.repository.RunMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class EvaluationRunService {

    private final EvaluationRunRepository repository;
    private final ExperimentService experimentService;
    private final AgentConfigService agentConfigService;
    private final EvaluationResultRepository resultRepository;
    private final RunMetricRepository metricRepository;
    private final RunArtifactStorage artifactStorage;

    public EvaluationRunService(
            EvaluationRunRepository repository,
            ExperimentService experimentService,
            AgentConfigService agentConfigService,
            EvaluationResultRepository resultRepository,
            RunMetricRepository metricRepository,
            RunArtifactStorage artifactStorage
    ) {
        this.repository = repository;
        this.experimentService = experimentService;
        this.agentConfigService = agentConfigService;
        this.resultRepository = resultRepository;
        this.metricRepository = metricRepository;
        this.artifactStorage = artifactStorage;
    }

    @Transactional(readOnly = true)
    public List<EvaluationRunDtos.SummaryResponse> list(
            Long experimentId,
            RunStatus status,
            RunSource source,
            String caseId,
            String model
    ) {
        List<EvaluationRun> runs = repository.findAllByOrderByStartedAtDescIdDesc().stream()
                .filter(run -> experimentId == null || run.getExperiment().getId().equals(experimentId))
                .filter(run -> status == null || run.getStatus() == status)
                .filter(run -> source == null || run.getSource() == source)
                .filter(run -> caseId == null || caseId.equals(run.getCaseId()))
                .filter(run -> model == null || model.equals(run.getAgentModel()))
                .toList();
        if (runs.isEmpty()) {
            return List.of();
        }
        List<Long> runIds = runs.stream().map(EvaluationRun::getId).toList();
        Map<Long, EvaluationResult> results = resultRepository.findAllByRunIdIn(runIds).stream()
                .collect(Collectors.toMap(result -> result.getRun().getId(), Function.identity()));
        Map<Long, RunMetric> metrics = metricRepository.findAllByRunIdIn(runIds).stream()
                .collect(Collectors.toMap(metric -> metric.getRun().getId(), Function.identity()));
        return runs.stream()
                .map(run -> toSummary(run, results.get(run.getId()), metrics.get(run.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public EvaluationRunDtos.DetailResponse getDetail(Long id) {
        EvaluationRun run = getEntity(id);
        EvaluationResult result = resultRepository.findByRunId(id).orElse(null);
        RunMetric metric = metricRepository.findByRunId(id).orElse(null);
        return new EvaluationRunDtos.DetailResponse(
                toResponse(run),
                result == null ? null : result.getScore(),
                metric == null ? null : metric.getLatencyMs(),
                metric == null ? null : metric.getPromptTokens(),
                metric == null ? null : metric.getCompletionTokens(),
                metric == null ? null : metric.getTotalTokens(),
                metric == null ? null : metric.getToolCallCount(),
                metric == null ? null : metric.getMutatingToolCallCount(),
                metric == null ? null : metric.getFailedToolCallCount(),
                artifactStorage.read(run.getArtifactPath())
        );
    }

    public EvaluationRunDtos.Response create(EvaluationRunDtos.Request request) {
        EvaluationRun run = new EvaluationRun();
        applyRequest(run, request);
        return toResponse(repository.save(run));
    }

    public EvaluationRunDtos.Response update(Long id, EvaluationRunDtos.Request request) {
        EvaluationRun run = getEntity(id);
        requireMutable(run);
        applyRequest(run, request);
        run.setUpdatedAt(Instant.now());
        return toResponse(repository.save(run));
    }

    public void delete(Long id) {
        EvaluationRun run = getEntity(id);
        requireMutable(run);
        repository.delete(run);
    }

    private void requireMutable(EvaluationRun run) {
        if (run.getSource() == RunSource.NETAGENT_BENCHMARK_IMPORT) {
            throw new ConflictException("Imported benchmark runs are immutable");
        }
    }

    @Transactional(readOnly = true)
    public EvaluationRun getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Evaluation run not found: " + id));
    }

    private void applyRequest(EvaluationRun run, EvaluationRunDtos.Request request) {
        Experiment experiment = experimentService.getEntity(request.experimentId());
        AgentConfig config = request.agentConfigId() == null ? null : agentConfigService.getEntity(request.agentConfigId());
        run.setExperiment(experiment);
        run.setAgentConfig(config);
        run.setSource(request.source());
        run.setTask(request.task());
        run.setStatus(request.status() == null ? RunStatus.PENDING : request.status());
        run.setFypRunId(request.runId());
        run.setArtifactPath(request.artifactPath());
        run.setExitCode(request.exitCode());
        run.setStartedAt(request.startedAt());
        run.setFinishedAt(request.finishedAt());
        run.setErrorMessage(request.errorMessage());
    }

    private EvaluationRunDtos.Response toResponse(EvaluationRun run) {
        AgentConfig config = run.getAgentConfig();
        return new EvaluationRunDtos.Response(
                run.getId(),
                run.getExperiment().getId(),
                run.getExperiment().getName(),
                config == null ? null : config.getId(),
                config == null ? null : config.getName(),
                run.getSource(),
                run.getTask(),
                run.getStatus(),
                run.getFypRunId(),
                run.getSchemaVersion(),
                run.getCaseId(),
                run.getTaskMode(),
                run.getAgentProvider(),
                run.getAgentModel(),
                run.getArtifactPath(),
                run.getExitCode(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getErrorMessage(),
                run.getCreatedAt(),
                run.getUpdatedAt()
        );
    }

    private EvaluationRunDtos.SummaryResponse toSummary(
            EvaluationRun run,
            EvaluationResult result,
            RunMetric metric
    ) {
        return new EvaluationRunDtos.SummaryResponse(
                toResponse(run),
                result == null ? null : result.getScore(),
                metric == null ? null : metric.getLatencyMs(),
                metric == null ? null : metric.getTotalTokens(),
                metric == null ? null : metric.getToolCallCount()
        );
    }
}
