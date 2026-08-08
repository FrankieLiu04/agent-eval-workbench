package com.frankliu.agentworkbench.service;

import com.frankliu.agentworkbench.api.dto.BenchmarkJobDtos;
import com.frankliu.agentworkbench.api.error.NotFoundException;
import com.frankliu.agentworkbench.domain.BenchmarkJob;
import com.frankliu.agentworkbench.domain.BenchmarkJobStatus;
import com.frankliu.agentworkbench.domain.EvaluationResult;
import com.frankliu.agentworkbench.domain.RunMetric;
import com.frankliu.agentworkbench.repository.BenchmarkJobRepository;
import com.frankliu.agentworkbench.repository.EvaluationResultRepository;
import com.frankliu.agentworkbench.repository.RunMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BenchmarkComparisonService {

    private final BenchmarkJobRepository jobRepository;
    private final EvaluationResultRepository resultRepository;
    private final RunMetricRepository metricRepository;
    private final BenchmarkJobService jobService;

    public BenchmarkComparisonService(
            BenchmarkJobRepository jobRepository,
            EvaluationResultRepository resultRepository,
            RunMetricRepository metricRepository,
            BenchmarkJobService jobService
    ) {
        this.jobRepository = jobRepository;
        this.resultRepository = resultRepository;
        this.metricRepository = metricRepository;
        this.jobService = jobService;
    }

    public BenchmarkJobDtos.BatchComparisonResponse compare(String batchId) {
        List<BenchmarkJob> jobs = jobRepository.findAllByBatchIdOrderByIdAsc(batchId);
        if (jobs.isEmpty()) {
            throw new NotFoundException("Benchmark batch not found: " + batchId);
        }
        List<Long> runIds = jobs.stream()
                .map(BenchmarkJob::getEvaluationRun)
                .filter(Objects::nonNull)
                .map(run -> run.getId())
                .toList();
        Map<Long, EvaluationResult> results = results(runIds);
        Map<Long, RunMetric> metrics = metrics(runIds);
        Map<Long, List<BenchmarkJob>> byProfile = jobs.stream().collect(Collectors.groupingBy(
                job -> job.getAgentConfig().getId(), LinkedHashMap::new, Collectors.toList()));
        List<BenchmarkJobDtos.ProfileComparison> profiles = byProfile.values().stream()
                .map(profileJobs -> profile(profileJobs, results, metrics))
                .toList();
        BenchmarkJob first = jobs.getFirst();
        int active = (int) jobs.stream().filter(this::active).count();
        int repetitions = jobs.stream().map(BenchmarkJob::getRepetition)
                .filter(Objects::nonNull).mapToInt(Integer::intValue).max().orElse(1);
        return new BenchmarkJobDtos.BatchComparisonResponse(
                batchId, first.getExperiment().getId(), first.getBenchmarkCase().getCaseId(),
                first.getCaseTitle(), first.getCaseSchemaVersion(), repetitions, jobs.size(), active,
                jobs.size() - active, profiles, jobs.stream().map(jobService::toResponse).toList());
    }

    private BenchmarkJobDtos.ProfileComparison profile(
            List<BenchmarkJob> jobs,
            Map<Long, EvaluationResult> results,
            Map<Long, RunMetric> metrics
    ) {
        BenchmarkJob first = jobs.getFirst();
        List<EvaluationResult> profileResults = values(jobs, results);
        List<RunMetric> profileMetrics = values(jobs, metrics);
        int evaluated = (int) profileResults.stream().filter(result -> result.getPassed() != null).count();
        int passed = (int) profileResults.stream().filter(result -> Boolean.TRUE.equals(result.getPassed())).count();
        int executionFailures = count(jobs, BenchmarkJobStatus.FAILED);
        int timedOut = count(jobs, BenchmarkJobStatus.TIMED_OUT);
        int cancelled = count(jobs, BenchmarkJobStatus.CANCELLED);
        int attempted = evaluated + executionFailures + timedOut;
        long toolCalls = sum(profileMetrics, RunMetric::getToolCallCount);
        long failedCalls = sum(profileMetrics, RunMetric::getFailedToolCallCount);
        long duplicateCalls = sum(profileMetrics, RunMetric::getDuplicateToolCallCount);
        return new BenchmarkJobDtos.ProfileComparison(
                first.getAgentConfig().getId(), first.getAgentConfigName(), first.getProvider(), first.getModel(),
                first.getPromptVersion(), first.getToolExposure(), first.getReasoningMode(), jobs.size(),
                count(jobs, BenchmarkJobStatus.QUEUED), countActive(jobs), evaluated, passed,
                executionFailures, timedOut, cancelled, attempted, ratio(passed, evaluated), ratio(passed, attempted),
                average(profileResults.stream().map(EvaluationResult::getScore).toList()),
                average(profileMetrics.stream().map(RunMetric::getLatencyMs).toList()),
                average(profileMetrics.stream().map(RunMetric::getTotalTokens).toList()),
                average(profileMetrics.stream().map(RunMetric::getAgentStepCount).toList()),
                average(profileMetrics.stream().map(RunMetric::getToolCallCount).toList()),
                average(profileMetrics.stream().map(RunMetric::getFailedToolCallCount).toList()),
                average(profileMetrics.stream().map(RunMetric::getDuplicateToolCallCount).toList()),
                toolCalls == 0 ? null : ratio(toolCalls - failedCalls, toolCalls),
                toolCalls == 0 ? null : ratio(duplicateCalls, toolCalls));
    }

    private long sum(List<RunMetric> metrics, Function<RunMetric, Integer> extractor) {
        return metrics.stream().map(extractor).filter(Objects::nonNull).mapToLong(Integer::longValue).sum();
    }

    private <T> List<T> values(List<BenchmarkJob> jobs, Map<Long, T> byRunId) {
        return jobs.stream().filter(job -> job.getStatus() == BenchmarkJobStatus.SUCCEEDED)
                .map(BenchmarkJob::getEvaluationRun).filter(Objects::nonNull)
                .map(run -> byRunId.get(run.getId())).filter(Objects::nonNull).toList();
    }

    private Map<Long, EvaluationResult> results(List<Long> runIds) {
        return runIds.isEmpty() ? Map.of() : resultRepository.findAllByRunIdIn(runIds).stream()
                .collect(Collectors.toMap(result -> result.getRun().getId(), Function.identity()));
    }

    private Map<Long, RunMetric> metrics(List<Long> runIds) {
        return runIds.isEmpty() ? Map.of() : metricRepository.findAllByRunIdIn(runIds).stream()
                .collect(Collectors.toMap(metric -> metric.getRun().getId(), Function.identity()));
    }

    private int count(List<BenchmarkJob> jobs, BenchmarkJobStatus status) {
        return (int) jobs.stream().filter(job -> job.getStatus() == status).count();
    }

    private int countActive(List<BenchmarkJob> jobs) {
        return (int) jobs.stream().filter(this::running).count();
    }

    private boolean active(BenchmarkJob job) {
        return job.getStatus() == BenchmarkJobStatus.QUEUED || running(job);
    }

    private boolean running(BenchmarkJob job) {
        return job.getStatus() == BenchmarkJobStatus.RUNNING
                || job.getStatus() == BenchmarkJobStatus.CANCEL_REQUESTED;
    }

    private BigDecimal average(List<? extends Number> values) {
        List<BigDecimal> present = values.stream().filter(Objects::nonNull)
                .map(value -> new BigDecimal(value.toString())).toList();
        if (present.isEmpty()) {
            return null;
        }
        BigDecimal total = present.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(present.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(long numerator, long denominator) {
        return denominator == 0 ? null : BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }
}
