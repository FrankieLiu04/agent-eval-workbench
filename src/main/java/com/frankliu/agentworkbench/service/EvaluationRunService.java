package com.frankliu.agentworkbench.service;

import com.frankliu.agentworkbench.api.dto.EvaluationRunDtos;
import com.frankliu.agentworkbench.api.error.NotFoundException;
import com.frankliu.agentworkbench.domain.AgentConfig;
import com.frankliu.agentworkbench.domain.EvaluationRun;
import com.frankliu.agentworkbench.domain.Experiment;
import com.frankliu.agentworkbench.domain.RunSource;
import com.frankliu.agentworkbench.domain.RunStatus;
import com.frankliu.agentworkbench.repository.EvaluationRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class EvaluationRunService {

    private final EvaluationRunRepository repository;
    private final ExperimentService experimentService;
    private final AgentConfigService agentConfigService;

    public EvaluationRunService(
            EvaluationRunRepository repository,
            ExperimentService experimentService,
            AgentConfigService agentConfigService
    ) {
        this.repository = repository;
        this.experimentService = experimentService;
        this.agentConfigService = agentConfigService;
    }

    @Transactional(readOnly = true)
    public List<EvaluationRunDtos.Response> list(Long experimentId, RunStatus status, RunSource source) {
        return repository.findAll().stream()
                .filter(run -> experimentId == null || run.getExperiment().getId().equals(experimentId))
                .filter(run -> status == null || run.getStatus() == status)
                .filter(run -> source == null || run.getSource() == source)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EvaluationRunDtos.Response get(Long id) {
        return toResponse(getEntity(id));
    }

    public EvaluationRunDtos.Response create(EvaluationRunDtos.Request request) {
        EvaluationRun run = new EvaluationRun();
        applyRequest(run, request);
        return toResponse(repository.save(run));
    }

    public EvaluationRunDtos.Response update(Long id, EvaluationRunDtos.Request request) {
        EvaluationRun run = getEntity(id);
        applyRequest(run, request);
        run.setUpdatedAt(Instant.now());
        return toResponse(repository.save(run));
    }

    public void delete(Long id) {
        EvaluationRun run = getEntity(id);
        repository.delete(run);
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
        run.setFypRunId(request.fypRunId());
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
                run.getArtifactPath(),
                run.getExitCode(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getErrorMessage(),
                run.getCreatedAt(),
                run.getUpdatedAt()
        );
    }
}
