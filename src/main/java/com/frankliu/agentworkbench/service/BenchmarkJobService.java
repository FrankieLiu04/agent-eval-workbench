package com.frankliu.agentworkbench.service;

import com.frankliu.agentworkbench.api.dto.BenchmarkJobDtos;
import com.frankliu.agentworkbench.api.dto.NetagentRunArtifact;
import com.frankliu.agentworkbench.api.error.ConflictException;
import com.frankliu.agentworkbench.api.error.NotFoundException;
import com.frankliu.agentworkbench.domain.AgentConfig;
import com.frankliu.agentworkbench.domain.AgentProvider;
import com.frankliu.agentworkbench.domain.BenchmarkCase;
import com.frankliu.agentworkbench.domain.BenchmarkJob;
import com.frankliu.agentworkbench.domain.BenchmarkJobStatus;
import com.frankliu.agentworkbench.domain.EvaluationRun;
import com.frankliu.agentworkbench.domain.Experiment;
import com.frankliu.agentworkbench.domain.ReasoningMode;
import com.frankliu.agentworkbench.repository.BenchmarkJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class BenchmarkJobService {

    private static final Set<String> DEEPSEEK_MODELS = Set.of("deepseek-v4-flash", "deepseek-v4-pro");

    private final BenchmarkJobRepository repository;
    private final ExperimentService experimentService;
    private final BenchmarkCaseService caseService;
    private final AgentConfigService agentConfigService;
    private final NetagentRunImportService importService;
    private final int jobTimeoutSeconds;

    public BenchmarkJobService(
            BenchmarkJobRepository repository,
            ExperimentService experimentService,
            BenchmarkCaseService caseService,
            AgentConfigService agentConfigService,
            NetagentRunImportService importService,
            @Value("${app.benchmark.job-timeout-seconds:300}") int jobTimeoutSeconds
    ) {
        this.repository = repository;
        this.experimentService = experimentService;
        this.caseService = caseService;
        this.agentConfigService = agentConfigService;
        this.importService = importService;
        this.jobTimeoutSeconds = jobTimeoutSeconds;
    }

    public BenchmarkJobDtos.JobResponse launch(BenchmarkJobDtos.LaunchRequest request) {
        BenchmarkCase benchmarkCase = caseService.getEntity(request.caseId());
        validateCase(benchmarkCase);
        AgentConfig config = agentConfigService.getEntity(request.agentConfigId());
        validateLaunchConfig(config);
        BenchmarkJob job = createJob(
                experimentService.getEntity(request.experimentId()), benchmarkCase, config,
                UUID.randomUUID().toString(), 1);
        return toResponse(repository.save(job));
    }

    public BenchmarkJobDtos.BatchLaunchResponse launchBatch(BenchmarkJobDtos.BatchLaunchRequest request) {
        Set<Long> uniqueIds = new HashSet<>(request.agentConfigIds());
        if (uniqueIds.size() != request.agentConfigIds().size()) {
            throw new ConflictException("A benchmark batch cannot contain duplicate model profiles");
        }
        BenchmarkCase benchmarkCase = caseService.getEntity(request.caseId());
        validateCase(benchmarkCase);
        Experiment experiment = experimentService.getEntity(request.experimentId());
        List<AgentConfig> configs = request.agentConfigIds().stream()
                .map(agentConfigService::getEntity)
                .peek(this::validateLaunchConfig)
                .toList();
        String batchId = UUID.randomUUID().toString();
        List<BenchmarkJob> jobs = configs.stream()
                .flatMap(config -> java.util.stream.IntStream.rangeClosed(1, request.repetitions())
                        .mapToObj(repetition -> createJob(experiment, benchmarkCase, config, batchId, repetition)))
                .toList();
        List<BenchmarkJobDtos.JobResponse> responses = repository.saveAll(jobs).stream()
                .map(this::toResponse)
                .toList();
        return new BenchmarkJobDtos.BatchLaunchResponse(batchId, responses.size(), responses);
    }

    @Transactional(readOnly = true)
    public List<BenchmarkJobDtos.JobResponse> list() {
        return repository.findAllByOrderByCreatedAtDescIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BenchmarkJobDtos.JobResponse get(Long id) {
        return toResponse(repository.findById(id).orElseThrow(() -> notFound(id)));
    }

    public BenchmarkJobDtos.JobResponse cancel(Long id) {
        BenchmarkJob job = lock(id);
        Instant now = Instant.now();
        if (job.getStatus() == BenchmarkJobStatus.QUEUED) {
            finish(job, BenchmarkJobStatus.CANCELLED, now, null);
        } else if (job.getStatus() == BenchmarkJobStatus.RUNNING) {
            job.setStatus(BenchmarkJobStatus.CANCEL_REQUESTED);
        } else if (job.getStatus() != BenchmarkJobStatus.CANCELLED
                && job.getStatus() != BenchmarkJobStatus.CANCEL_REQUESTED) {
            throw new ConflictException("Benchmark job cannot be cancelled from " + job.getStatus());
        }
        return toResponse(job);
    }

    public Optional<BenchmarkJobDtos.ClaimResponse> claim(String workerId) {
        return repository.findFirstByStatusOrderByCreatedAtAscIdAsc(BenchmarkJobStatus.QUEUED)
                .map(job -> claim(job, workerId));
    }

    public BenchmarkJobDtos.WorkerStatusResponse heartbeat(Long id, String workerId, String claimToken) {
        BenchmarkJob job = lockWorkerJob(id, workerId, claimToken);
        if (isActive(job)) {
            job.setHeartbeatAt(Instant.now());
        }
        return workerStatus(job);
    }

    public BenchmarkJobDtos.JobResponse complete(
            Long id,
            String workerId,
            String claimToken,
            JsonNode artifactJson
    ) {
        BenchmarkJob job = lockWorkerJob(id, workerId, claimToken);
        if (job.getStatus() == BenchmarkJobStatus.CANCEL_REQUESTED) {
            finish(job, BenchmarkJobStatus.CANCELLED, Instant.now(), null);
            return toResponse(job);
        }
        if (job.getStatus() != BenchmarkJobStatus.RUNNING) {
            return toResponse(job);
        }
        NetagentRunArtifact artifact = importService.inspect(artifactJson);
        verifyArtifact(job, artifact);
        EvaluationRun run = importService.importArtifact(
                job.getExperiment().getId(), job.getAgentConfig().getId(), artifact, artifactJson);
        job.setEvaluationRun(run);
        BenchmarkJobStatus status = "completed".equals(artifact.result().status())
                ? BenchmarkJobStatus.SUCCEEDED : BenchmarkJobStatus.FAILED;
        finish(job, status, Instant.now(), artifact.result().errorMessage());
        return toResponse(job);
    }

    public BenchmarkJobDtos.JobResponse fail(
            Long id,
            String workerId,
            String claimToken,
            BenchmarkJobDtos.FailureRequest request
    ) {
        BenchmarkJob job = lockWorkerJob(id, workerId, claimToken);
        if (job.getStatus() == BenchmarkJobStatus.CANCEL_REQUESTED) {
            finish(job, BenchmarkJobStatus.CANCELLED, Instant.now(), null);
            return toResponse(job);
        }
        if (job.getStatus() != BenchmarkJobStatus.RUNNING) {
            return toResponse(job);
        }
        BenchmarkJobStatus status = request.timedOut()
                ? BenchmarkJobStatus.TIMED_OUT : BenchmarkJobStatus.FAILED;
        finish(job, status, Instant.now(), request.message());
        return toResponse(job);
    }

    public BenchmarkJobDtos.JobResponse cancelled(Long id, String workerId, String claimToken) {
        BenchmarkJob job = lockWorkerJob(id, workerId, claimToken);
        if (job.getStatus() == BenchmarkJobStatus.CANCELLED) {
            return toResponse(job);
        }
        if (job.getStatus() != BenchmarkJobStatus.CANCEL_REQUESTED) {
            throw new ConflictException("Benchmark job has no cancellation request: " + id);
        }
        finish(job, BenchmarkJobStatus.CANCELLED, Instant.now(), null);
        return toResponse(job);
    }

    private BenchmarkJobDtos.ClaimResponse claim(BenchmarkJob job, String workerId) {
        Instant now = Instant.now();
        job.setStatus(BenchmarkJobStatus.RUNNING);
        job.setWorkerId(workerId);
        job.setClaimToken(UUID.randomUUID().toString());
        job.setStartedAt(now);
        job.setHeartbeatAt(now);
        return new BenchmarkJobDtos.ClaimResponse(
                job.getId(), job.getClaimToken(), job.getBenchmarkCase().getCaseId(),
                externalProvider(job.getProvider()), job.getModel(), job.getReasoningMode(),
                job.getMaxTurns(), job.getTimeoutSeconds());
    }

    private BenchmarkJob createJob(
            Experiment experiment,
            BenchmarkCase benchmarkCase,
            AgentConfig config,
            String batchId,
            int repetition
    ) {
        BenchmarkJob job = new BenchmarkJob();
        job.setExperiment(experiment);
        job.setBenchmarkCase(benchmarkCase);
        job.setAgentConfig(config);
        job.setBatchId(batchId);
        job.setRepetition(repetition);
        job.setCaseTitle(benchmarkCase.getTitle());
        job.setCaseSchemaVersion(benchmarkCase.getSchemaVersion());
        job.setAgentConfigName(config.getName());
        job.setProvider(config.getProvider());
        job.setModel(externalModel(config));
        job.setPromptVersion(config.getPromptVersion());
        job.setToolExposure(config.getToolExposure());
        job.setReasoningMode(effectiveReasoningMode(config));
        job.setMaxTurns(config.getMaxSteps());
        job.setTimeoutSeconds(jobTimeoutSeconds);
        return job;
    }

    private void validateCase(BenchmarkCase benchmarkCase) {
        if (!benchmarkCase.isEnabled()) {
            throw new ConflictException("Benchmark case is disabled: " + benchmarkCase.getCaseId());
        }
    }

    private void validateLaunchConfig(AgentConfig config) {
        if (config.getProvider() == AgentProvider.OTHER) {
            throw new ConflictException("Agent provider OTHER cannot launch benchmark jobs");
        }
        if (config.getProvider() == AgentProvider.DEEPSEEK
                && !DEEPSEEK_MODELS.contains(config.getModelName())) {
            throw new ConflictException("DeepSeek model must be deepseek-v4-flash or deepseek-v4-pro");
        }
        if (config.getProvider() == AgentProvider.DEEPSEEK
                && "deepseek-v4-pro".equals(config.getModelName())
                && effectiveReasoningMode(config) == ReasoningMode.LOW) {
            throw new ConflictException("deepseek-v4-pro does not support LOW reasoning mode");
        }
        if (config.getProvider() == AgentProvider.LOCAL_MOCK && config.getMaxSteps() < 7) {
            throw new ConflictException("LOCAL_MOCK requires maxSteps of at least 7 for this benchmark case");
        }
        if (config.getProvider() != AgentProvider.DEEPSEEK
                && effectiveReasoningMode(config) != ReasoningMode.DISABLED) {
            throw new ConflictException("Reasoning LOW, HIGH, and MAX are supported only for DeepSeek profiles");
        }
    }

    private BenchmarkJob lockWorkerJob(Long id, String workerId, String claimToken) {
        BenchmarkJob job = lock(id);
        if (job.getWorkerId() == null || job.getClaimToken() == null
                || !job.getWorkerId().equals(workerId) || !job.getClaimToken().equals(claimToken)) {
            throw new ConflictException("Benchmark job claim credentials do not match: " + id);
        }
        if (job.getStatus() == BenchmarkJobStatus.RUNNING && deadlineReached(job, Instant.now())) {
            finish(job, BenchmarkJobStatus.TIMED_OUT, Instant.now(), "Benchmark job exceeded its timeout");
        }
        return job;
    }

    private void verifyArtifact(BenchmarkJob job, NetagentRunArtifact artifact) {
        if (!"case-run".equals(artifact.task().mode())
                || !job.getBenchmarkCase().getCaseId().equals(artifact.task().caseId())
                || !externalProvider(job.getProvider()).equals(artifact.agent().provider())
                || !job.getModel().equals(artifact.agent().model())
                || !job.getReasoningMode().name().equals(artifact.agent().reasoningMode())) {
            throw new ConflictException("Netagent artifact does not match the claimed benchmark selection");
        }
    }

    private String externalProvider(AgentProvider provider) {
        return switch (provider) {
            case LOCAL_MOCK -> "scripted";
            case DEEPSEEK -> "deepseek";
            case OPENAI_COMPATIBLE -> "openai";
            case OTHER -> throw new ConflictException("Agent provider OTHER cannot run benchmark jobs");
        };
    }

    private String externalModel(AgentConfig config) {
        return config.getProvider() == AgentProvider.LOCAL_MOCK ? "case-reference" : config.getModelName();
    }

    private ReasoningMode effectiveReasoningMode(AgentConfig config) {
        if (config.getProvider() == AgentProvider.LOCAL_MOCK || config.getReasoningMode() == ReasoningMode.DEFAULT) {
            return ReasoningMode.DISABLED;
        }
        return config.getReasoningMode();
    }

    private boolean isActive(BenchmarkJob job) {
        return job.getStatus() == BenchmarkJobStatus.RUNNING
                || job.getStatus() == BenchmarkJobStatus.CANCEL_REQUESTED;
    }

    private boolean deadlineReached(BenchmarkJob job, Instant now) {
        return !job.getStartedAt().plusSeconds(job.getTimeoutSeconds()).isAfter(now);
    }

    private BenchmarkJob lock(Long id) {
        return repository.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
    }

    private NotFoundException notFound(Long id) {
        return new NotFoundException("Benchmark job not found: " + id);
    }

    private void finish(BenchmarkJob job, BenchmarkJobStatus status, Instant now, String errorMessage) {
        job.setStatus(status);
        job.setFinishedAt(now);
        job.setErrorMessage(clip(errorMessage));
    }

    private String clip(String value) {
        return value == null || value.length() <= 2000 ? value : value.substring(0, 2000);
    }

    private BenchmarkJobDtos.WorkerStatusResponse workerStatus(BenchmarkJob job) {
        return new BenchmarkJobDtos.WorkerStatusResponse(
                job.getStatus(), job.getStatus() == BenchmarkJobStatus.CANCEL_REQUESTED);
    }

    BenchmarkJobDtos.JobResponse toResponse(BenchmarkJob job) {
        return new BenchmarkJobDtos.JobResponse(
            job.getId(), job.getStatus(), job.getExperiment().getId(), job.getExperiment().getName(),
                job.getBenchmarkCase().getCaseId(), job.getCaseTitle(), job.getCaseSchemaVersion(),
                job.getAgentConfig().getId(), job.getAgentConfigName(), job.getProvider(), job.getModel(),
                job.getPromptVersion(), job.getToolExposure(), job.getReasoningMode(),
                job.getBatchId(), job.getRepetition(),
                job.getEvaluationRun() == null ? null : job.getEvaluationRun().getId(), job.getWorkerId(),
                job.getCreatedAt(), job.getStartedAt(), job.getHeartbeatAt(), job.getFinishedAt(),
                job.getErrorMessage());
    }
}
