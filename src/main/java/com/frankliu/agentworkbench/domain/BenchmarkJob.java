package com.frankliu.agentworkbench.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "benchmark_jobs", indexes = @Index(name = "idx_benchmark_jobs_batch", columnList = "batch_id"))
public class BenchmarkJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BenchmarkJobStatus status = BenchmarkJobStatus.QUEUED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private BenchmarkCase benchmarkCase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_config_id", nullable = false)
    private AgentConfig agentConfig;

    @Column(length = 36, updatable = false)
    private String batchId;

    @Column(updatable = false)
    private Integer repetition;

    @Column(length = 240, updatable = false)
    private String caseTitle;

    @Column(length = 40, updatable = false)
    private String caseSchemaVersion;

    @Column(length = 160, updatable = false)
    private String agentConfigName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private AgentProvider provider;

    @Column(nullable = false, length = 160, updatable = false)
    private String model;

    @Column(length = 120, updatable = false)
    private String promptVersion;

    @Column(length = 80, updatable = false)
    private String toolExposure;

    @Enumerated(EnumType.STRING)
    @Column(length = 40, updatable = false)
    private ReasoningMode reasoningMode;

    @Column(nullable = false, updatable = false)
    private Integer maxTurns;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_run_id", unique = true)
    private EvaluationRun evaluationRun;

    @Column(length = 160)
    private String workerId;

    @Column(length = 36)
    private String claimToken;

    @Column(nullable = false, updatable = false)
    private Integer timeoutSeconds;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant startedAt;

    private Instant heartbeatAt;

    private Instant finishedAt;

    @Column(length = 2000)
    private String errorMessage;

    public Long getId() {
        return id;
    }

    public BenchmarkJobStatus getStatus() {
        return status;
    }

    public void setStatus(BenchmarkJobStatus status) {
        this.status = status;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public BenchmarkCase getBenchmarkCase() {
        return benchmarkCase;
    }

    public void setBenchmarkCase(BenchmarkCase benchmarkCase) {
        this.benchmarkCase = benchmarkCase;
    }

    public AgentConfig getAgentConfig() {
        return agentConfig;
    }

    public void setAgentConfig(AgentConfig agentConfig) {
        this.agentConfig = agentConfig;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public Integer getRepetition() {
        return repetition;
    }

    public void setRepetition(Integer repetition) {
        this.repetition = repetition;
    }

    public String getCaseTitle() {
        return caseTitle;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

    public String getCaseSchemaVersion() {
        return caseSchemaVersion;
    }

    public void setCaseSchemaVersion(String caseSchemaVersion) {
        this.caseSchemaVersion = caseSchemaVersion;
    }

    public String getAgentConfigName() {
        return agentConfigName;
    }

    public void setAgentConfigName(String agentConfigName) {
        this.agentConfigName = agentConfigName;
    }

    public AgentProvider getProvider() {
        return provider;
    }

    public void setProvider(AgentProvider provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getToolExposure() {
        return toolExposure;
    }

    public void setToolExposure(String toolExposure) {
        this.toolExposure = toolExposure;
    }

    public ReasoningMode getReasoningMode() {
        return reasoningMode;
    }

    public void setReasoningMode(ReasoningMode reasoningMode) {
        this.reasoningMode = reasoningMode;
    }

    public Integer getMaxTurns() {
        return maxTurns;
    }

    public void setMaxTurns(Integer maxTurns) {
        this.maxTurns = maxTurns;
    }

    public EvaluationRun getEvaluationRun() {
        return evaluationRun;
    }

    public void setEvaluationRun(EvaluationRun evaluationRun) {
        this.evaluationRun = evaluationRun;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getClaimToken() {
        return claimToken;
    }

    public void setClaimToken(String claimToken) {
        this.claimToken = claimToken;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getHeartbeatAt() {
        return heartbeatAt;
    }

    public void setHeartbeatAt(Instant heartbeatAt) {
        this.heartbeatAt = heartbeatAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
