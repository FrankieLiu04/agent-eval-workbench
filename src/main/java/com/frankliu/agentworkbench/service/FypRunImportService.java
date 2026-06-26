package com.frankliu.agentworkbench.service;

import com.frankliu.agentworkbench.api.dto.EvaluationResultDtos;
import com.frankliu.agentworkbench.api.dto.EvaluationRunDtos;
import com.frankliu.agentworkbench.api.dto.FypRunImportDtos;
import com.frankliu.agentworkbench.api.dto.RunMetricDtos;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@Transactional
public class FypRunImportService {

    private final EvaluationRunRepository runRepository;
    private final EvaluationResultRepository resultRepository;
    private final RunMetricRepository metricRepository;
    private final ExperimentService experimentService;
    private final AgentConfigService agentConfigService;

    public FypRunImportService(
            EvaluationRunRepository runRepository,
            EvaluationResultRepository resultRepository,
            RunMetricRepository metricRepository,
            ExperimentService experimentService,
            AgentConfigService agentConfigService
    ) {
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.metricRepository = metricRepository;
        this.experimentService = experimentService;
        this.agentConfigService = agentConfigService;
    }

    public FypRunImportDtos.Response importRun(FypRunImportDtos.Request request) {
        String fypRunId = request.workbenchImport().runId();
        if (runRepository.existsByFypRunId(fypRunId)) {
            throw new ConflictException("FYP run already imported: " + fypRunId);
        }

        Experiment experiment = experimentService.getEntity(request.experimentId());
        AgentConfig config = request.agentConfigId() == null ? null : agentConfigService.getEntity(request.agentConfigId());
        RunStatus status = FypRunImportDtos.toRunStatus(request.result().status());

        EvaluationRun run = new EvaluationRun();
        run.setExperiment(experiment);
        run.setAgentConfig(config);
        run.setSource(sourceFor(request.benchmark()));
        run.setTask(request.workbenchImport().task());
        run.setStatus(status);
        run.setFypRunId(fypRunId);
        run.setArtifactPath(artifactPath(request.artifacts()));
        run.setStartedAt(request.workbenchImport().timestamp());
        run.setFinishedAt(status == RunStatus.COMPLETED || status == RunStatus.FAILED ? Instant.now() : null);
        run.setErrorMessage(request.result().errorMessage());
        EvaluationRun savedRun = runRepository.save(run);

        EvaluationResult savedResult = saveResult(savedRun, request);
        RunMetric savedMetric = saveMetric(savedRun, request.metrics());

        return new FypRunImportDtos.Response(
                toRunResponse(savedRun),
                toResultResponse(savedResult),
                toMetricResponse(savedMetric)
        );
    }

    private EvaluationResult saveResult(EvaluationRun run, FypRunImportDtos.Request request) {
        EvaluationResult result = new EvaluationResult();
        result.setRun(run);
        result.setScore(request.result().score());
        result.setAccuracy(request.result().score());
        result.setSummary(summary(request));
        return resultRepository.save(result);
    }

    private RunMetric saveMetric(EvaluationRun run, FypRunImportDtos.Metrics metrics) {
        RunMetric metric = new RunMetric();
        metric.setRun(run);
        if (metrics != null) {
            metric.setLatencyMs(toMillis(metrics.durationSeconds()));
            metric.setPromptTokens(metrics.promptTokens());
            metric.setCompletionTokens(metrics.completionTokens());
            metric.setTotalTokens(totalTokens(metrics));
            metric.setToolCallCount(metrics.toolCalls());
            metric.setMutatingToolCallCount(metrics.mutatingToolCalls());
            metric.setFailedToolCallCount(metrics.failedToolCalls());
        }
        return metricRepository.save(metric);
    }

    private RunSource sourceFor(FypRunImportDtos.Benchmark benchmark) {
        return "netconfeval".equalsIgnoreCase(benchmark.name()) ? RunSource.NETCONFEVAL_IMPORT : RunSource.FYP_AGENT_SERVICE;
    }

    private String artifactPath(FypRunImportDtos.Artifacts artifacts) {
        return artifacts == null ? null : artifacts.runLogPath();
    }

    private Long toMillis(BigDecimal seconds) {
        if (seconds == null) {
            return null;
        }
        return seconds.multiply(BigDecimal.valueOf(1000)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private Integer totalTokens(FypRunImportDtos.Metrics metrics) {
        if (metrics.totalTokens() != null) {
            return metrics.totalTokens();
        }
        if (metrics.promptTokens() == null && metrics.completionTokens() == null) {
            return null;
        }
        return (metrics.promptTokens() == null ? 0 : metrics.promptTokens())
                + (metrics.completionTokens() == null ? 0 : metrics.completionTokens());
    }

    private String summary(FypRunImportDtos.Request request) {
        String benchmark = request.benchmark().name();
        String level = request.benchmark().level();
        String caseId = request.benchmark().caseId();
        String finalAnswer = request.result().finalAnswer();
        String prefix = "Imported FYP artifact"
                + (benchmark == null ? "" : " for " + benchmark)
                + (level == null ? "" : "/" + level)
                + (caseId == null ? "" : " case " + caseId)
                + ".";
        if (finalAnswer == null || finalAnswer.isBlank()) {
            return prefix;
        }
        String clippedAnswer = finalAnswer.length() > 1500 ? finalAnswer.substring(0, 1500) : finalAnswer;
        return prefix + " " + clippedAnswer;
    }

    private EvaluationRunDtos.Response toRunResponse(EvaluationRun run) {
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

    private EvaluationResultDtos.Response toResultResponse(EvaluationResult result) {
        return new EvaluationResultDtos.Response(
                result.getId(),
                result.getRun().getId(),
                result.getScore(),
                result.getAccuracy(),
                result.getSuccessCount(),
                result.getFailCount(),
                result.getWrongCount(),
                result.getFormatErrorCount(),
                result.getSyntaxErrorCount(),
                result.getTestErrorCount(),
                result.getSummary(),
                result.getCreatedAt(),
                result.getUpdatedAt()
        );
    }

    private RunMetricDtos.Response toMetricResponse(RunMetric metric) {
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
