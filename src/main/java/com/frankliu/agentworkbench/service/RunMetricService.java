package com.frankliu.agentworkbench.service;

import com.frankliu.agentworkbench.api.dto.RunMetricDtos;
import com.frankliu.agentworkbench.api.error.ConflictException;
import com.frankliu.agentworkbench.api.error.NotFoundException;
import com.frankliu.agentworkbench.domain.EvaluationRun;
import com.frankliu.agentworkbench.domain.RunMetric;
import com.frankliu.agentworkbench.repository.RunMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class RunMetricService {

    private final RunMetricRepository repository;
    private final EvaluationRunService runService;

    public RunMetricService(RunMetricRepository repository, EvaluationRunService runService) {
        this.repository = repository;
        this.runService = runService;
    }

    @Transactional(readOnly = true)
    public List<RunMetricDtos.Response> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RunMetricDtos.Response get(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public RunMetricDtos.Response getByRun(Long runId) {
        return repository.findByRunId(runId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Run metric not found for run: " + runId));
    }

    public RunMetricDtos.Response create(RunMetricDtos.Request request) {
        if (repository.existsByRunId(request.runId())) {
            throw new ConflictException("Run metric already exists for run: " + request.runId());
        }
        RunMetric metric = new RunMetric();
        applyRequest(metric, request);
        return toResponse(repository.save(metric));
    }

    public RunMetricDtos.Response update(Long id, RunMetricDtos.Request request) {
        RunMetric metric = getEntity(id);
        applyRequest(metric, request);
        metric.setUpdatedAt(Instant.now());
        return toResponse(repository.save(metric));
    }

    public void delete(Long id) {
        RunMetric metric = getEntity(id);
        repository.delete(metric);
    }

    private RunMetric getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Run metric not found: " + id));
    }

    private void applyRequest(RunMetric metric, RunMetricDtos.Request request) {
        EvaluationRun run = runService.getEntity(request.runId());
        metric.setRun(run);
        metric.setLatencyMs(request.latencyMs());
        metric.setPromptTokens(request.promptTokens());
        metric.setCompletionTokens(request.completionTokens());
        metric.setTotalTokens(request.totalTokens());
        metric.setToolCallCount(request.toolCallCount());
        metric.setMutatingToolCallCount(request.mutatingToolCallCount());
        metric.setFailedToolCallCount(request.failedToolCallCount());
    }

    private RunMetricDtos.Response toResponse(RunMetric metric) {
        return new RunMetricDtos.Response(
                metric.getId(),
                metric.getRun().getId(),
                metric.getLatencyMs(),
                metric.getPromptTokens(),
                metric.getCompletionTokens(),
                metric.getTotalTokens(),
                metric.getToolCallCount(),
                metric.getMutatingToolCallCount(),
                metric.getFailedToolCallCount(),
                metric.getCreatedAt(),
                metric.getUpdatedAt()
        );
    }
}
