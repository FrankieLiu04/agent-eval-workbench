package com.frankliu.agentworkbench.config;

import com.frankliu.agentworkbench.domain.AgentConfig;
import com.frankliu.agentworkbench.domain.AgentProvider;
import com.frankliu.agentworkbench.domain.EvaluationResult;
import com.frankliu.agentworkbench.domain.EvaluationRun;
import com.frankliu.agentworkbench.domain.Experiment;
import com.frankliu.agentworkbench.domain.ExperimentDomain;
import com.frankliu.agentworkbench.domain.ExperimentStatus;
import com.frankliu.agentworkbench.domain.ReasoningMode;
import com.frankliu.agentworkbench.domain.RunMetric;
import com.frankliu.agentworkbench.domain.RunSource;
import com.frankliu.agentworkbench.domain.RunStatus;
import com.frankliu.agentworkbench.repository.AgentConfigRepository;
import com.frankliu.agentworkbench.repository.EvaluationResultRepository;
import com.frankliu.agentworkbench.repository.EvaluationRunRepository;
import com.frankliu.agentworkbench.repository.ExperimentRepository;
import com.frankliu.agentworkbench.repository.RunMetricRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;

@Configuration
public class SeedDataConfig {

    @Bean
    @ConditionalOnProperty(name = "app.seed-data.enabled", havingValue = "true")
    CommandLineRunner seedData(
            ExperimentRepository experimentRepository,
            AgentConfigRepository agentConfigRepository,
            EvaluationRunRepository runRepository,
            EvaluationResultRepository resultRepository,
            RunMetricRepository metricRepository
    ) {
        return args -> {
            if (experimentRepository.count() > 0) {
                return;
            }

            Experiment benchmark = createBenchmarkExperiment(experimentRepository);
            Experiment troubleshooting = createTroubleshootingExperiment(experimentRepository);
            AgentConfig config = createAgentConfig(agentConfigRepository);

            EvaluationRun benchmarkRun = createBenchmarkRun(runRepository, benchmark, config);
            createBenchmarkResult(resultRepository, benchmarkRun);
            createBenchmarkMetric(metricRepository, benchmarkRun);

            EvaluationRun agentRun = createAgentRun(runRepository, troubleshooting, config);
            createAgentMetric(metricRepository, agentRun);
        };
    }

    private Experiment createBenchmarkExperiment(ExperimentRepository repository) {
        Experiment experiment = new Experiment();
        experiment.setName("NetConfEval Step 1 Baseline");
        experiment.setDomain(ExperimentDomain.NETWORK_BENCHMARK);
        experiment.setDescription("Seed experiment for tracking FYP NetConfEval translation benchmark runs.");
        experiment.setDatasetName("netconfeval-step1-small-sample");
        experiment.setBaselineModel("deepseek-v4-flash");
        experiment.setStatus(ExperimentStatus.ACTIVE);
        return repository.save(experiment);
    }

    private Experiment createTroubleshootingExperiment(ExperimentRepository repository) {
        Experiment experiment = new Experiment();
        experiment.setName("CML Troubleshooting Agent Smoke");
        experiment.setDomain(ExperimentDomain.TROUBLESHOOTING_AGENT);
        experiment.setDescription("Seed experiment for FYP CML MCP agent troubleshooting runs.");
        experiment.setDatasetName("synthetic-cml-lab-smoke");
        experiment.setBaselineModel("deepseek-v4-flash");
        experiment.setStatus(ExperimentStatus.ACTIVE);
        return repository.save(experiment);
    }

    private AgentConfig createAgentConfig(AgentConfigRepository repository) {
        AgentConfig config = new AgentConfig();
        config.setName("DeepSeek V4 Flash Full Tool Access");
        config.setProvider(AgentProvider.DEEPSEEK);
        config.setModelName("deepseek-v4-flash");
        config.setPromptVersion("full_access_v1");
        config.setToolExposure("all");
        config.setMaxSteps(20);
        config.setReasoningMode(ReasoningMode.DEFAULT);
        return repository.save(config);
    }

    private EvaluationRun createBenchmarkRun(
            EvaluationRunRepository repository,
            Experiment experiment,
            AgentConfig config
    ) {
        EvaluationRun run = new EvaluationRun();
        run.setExperiment(experiment);
        run.setAgentConfig(config);
        run.setSource(RunSource.NETCONFEVAL_IMPORT);
        run.setTask("Translate synthetic reachability, waypoint, and load-balancing requirements.");
        run.setStatus(RunStatus.COMPLETED);
        run.setArtifactPath("../netagent-benchmark/experiments/netconfeval-paper/level1-2-20260626-001054/reproduction_summary.md");
        run.setStartedAt(Instant.parse("2026-06-25T16:15:24Z"));
        run.setFinishedAt(Instant.parse("2026-06-25T16:15:45Z"));
        return repository.save(run);
    }

    private void createBenchmarkResult(EvaluationResultRepository repository, EvaluationRun run) {
        EvaluationResult result = new EvaluationResult();
        result.setRun(run);
        result.setScore(new BigDecimal("0.8824"));
        result.setAccuracy(new BigDecimal("0.8824"));
        result.setSuccessCount(15);
        result.setFailCount(0);
        result.setWrongCount(0);
        result.setFormatErrorCount(2);
        result.setSyntaxErrorCount(0);
        result.setTestErrorCount(0);
        result.setSummary("Seeded from the FYP NetConfEval Step 1 small-sample reproduction summary.");
        repository.save(result);
    }

    private void createBenchmarkMetric(RunMetricRepository repository, EvaluationRun run) {
        RunMetric metric = new RunMetric();
        metric.setRun(run);
        metric.setLatencyMs(45839L);
        metric.setPromptTokens(6400);
        metric.setCompletionTokens(12450);
        metric.setTotalTokens(18850);
        metric.setToolCallCount(0);
        metric.setMutatingToolCallCount(0);
        metric.setFailedToolCallCount(0);
        repository.save(metric);
    }

    private EvaluationRun createAgentRun(
            EvaluationRunRepository repository,
            Experiment experiment,
            AgentConfig config
    ) {
        EvaluationRun run = new EvaluationRun();
        run.setExperiment(experiment);
        run.setAgentConfig(config);
        run.setSource(RunSource.FYP_AGENT_SERVICE);
        run.setTask("List CML labs and summarize visible MCP tools.");
        run.setStatus(RunStatus.COMPLETED);
        run.setFypRunId("seed-fyp-agent-run");
        run.setArtifactPath("../netagent-benchmark/experiments/runs/seed-fyp-agent-run/run.json");
        run.setStartedAt(Instant.parse("2026-06-26T00:00:00Z"));
        run.setFinishedAt(Instant.parse("2026-06-26T00:00:08Z"));
        return repository.save(run);
    }

    private void createAgentMetric(RunMetricRepository repository, EvaluationRun run) {
        RunMetric metric = new RunMetric();
        metric.setRun(run);
        metric.setLatencyMs(8200L);
        metric.setPromptTokens(1200);
        metric.setCompletionTokens(450);
        metric.setTotalTokens(1650);
        metric.setToolCallCount(2);
        metric.setMutatingToolCallCount(0);
        metric.setFailedToolCallCount(0);
        repository.save(metric);
    }
}
