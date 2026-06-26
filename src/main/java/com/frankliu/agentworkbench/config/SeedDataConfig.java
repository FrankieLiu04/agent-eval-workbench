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

            Experiment benchmark = new Experiment();
            benchmark.setName("NetConfEval Step 1 Baseline");
            benchmark.setDomain(ExperimentDomain.NETWORK_BENCHMARK);
            benchmark.setDescription("Seed experiment for tracking FYP NetConfEval translation benchmark runs.");
            benchmark.setDatasetName("netconfeval-step1-small-sample");
            benchmark.setBaselineModel("deepseek-v4-flash");
            benchmark.setStatus(ExperimentStatus.ACTIVE);
            benchmark = experimentRepository.save(benchmark);

            Experiment troubleshooting = new Experiment();
            troubleshooting.setName("CML Troubleshooting Agent Smoke");
            troubleshooting.setDomain(ExperimentDomain.TROUBLESHOOTING_AGENT);
            troubleshooting.setDescription("Seed experiment for FYP CML MCP agent troubleshooting runs.");
            troubleshooting.setDatasetName("synthetic-cml-lab-smoke");
            troubleshooting.setBaselineModel("deepseek-v4-flash");
            troubleshooting.setStatus(ExperimentStatus.ACTIVE);
            troubleshooting = experimentRepository.save(troubleshooting);

            AgentConfig config = new AgentConfig();
            config.setName("DeepSeek V4 Flash Full Tool Access");
            config.setProvider(AgentProvider.DEEPSEEK);
            config.setModelName("deepseek-v4-flash");
            config.setPromptVersion("full_access_v1");
            config.setToolExposure("all");
            config.setMaxSteps(20);
            config.setReasoningMode(ReasoningMode.DEFAULT);
            config = agentConfigRepository.save(config);

            EvaluationRun benchmarkRun = new EvaluationRun();
            benchmarkRun.setExperiment(benchmark);
            benchmarkRun.setAgentConfig(config);
            benchmarkRun.setSource(RunSource.NETCONFEVAL_IMPORT);
            benchmarkRun.setTask("Translate synthetic reachability, waypoint, and load-balancing requirements.");
            benchmarkRun.setStatus(RunStatus.COMPLETED);
            benchmarkRun.setArtifactPath("../netagent-benchmark/experiments/netconfeval-paper/level1-2-20260626-001054/reproduction_summary.md");
            benchmarkRun.setStartedAt(Instant.parse("2026-06-25T16:15:24Z"));
            benchmarkRun.setFinishedAt(Instant.parse("2026-06-25T16:15:45Z"));
            benchmarkRun = runRepository.save(benchmarkRun);

            EvaluationResult benchmarkResult = new EvaluationResult();
            benchmarkResult.setRun(benchmarkRun);
            benchmarkResult.setScore(new BigDecimal("0.8824"));
            benchmarkResult.setAccuracy(new BigDecimal("0.8824"));
            benchmarkResult.setSuccessCount(15);
            benchmarkResult.setFailCount(0);
            benchmarkResult.setWrongCount(0);
            benchmarkResult.setFormatErrorCount(2);
            benchmarkResult.setSyntaxErrorCount(0);
            benchmarkResult.setTestErrorCount(0);
            benchmarkResult.setSummary("Seeded from the FYP NetConfEval Step 1 small-sample reproduction summary.");
            resultRepository.save(benchmarkResult);

            RunMetric benchmarkMetric = new RunMetric();
            benchmarkMetric.setRun(benchmarkRun);
            benchmarkMetric.setLatencyMs(45839L);
            benchmarkMetric.setPromptTokens(6400);
            benchmarkMetric.setCompletionTokens(12450);
            benchmarkMetric.setTotalTokens(18850);
            benchmarkMetric.setToolCallCount(0);
            benchmarkMetric.setMutatingToolCallCount(0);
            benchmarkMetric.setFailedToolCallCount(0);
            metricRepository.save(benchmarkMetric);

            EvaluationRun agentRun = new EvaluationRun();
            agentRun.setExperiment(troubleshooting);
            agentRun.setAgentConfig(config);
            agentRun.setSource(RunSource.FYP_AGENT_SERVICE);
            agentRun.setTask("List CML labs and summarize visible MCP tools.");
            agentRun.setStatus(RunStatus.COMPLETED);
            agentRun.setFypRunId("seed-fyp-agent-run");
            agentRun.setArtifactPath("../netagent-benchmark/experiments/runs/seed-fyp-agent-run/run.json");
            agentRun.setStartedAt(Instant.parse("2026-06-26T00:00:00Z"));
            agentRun.setFinishedAt(Instant.parse("2026-06-26T00:00:08Z"));
            agentRun = runRepository.save(agentRun);

            RunMetric agentMetric = new RunMetric();
            agentMetric.setRun(agentRun);
            agentMetric.setLatencyMs(8200L);
            agentMetric.setPromptTokens(1200);
            agentMetric.setCompletionTokens(450);
            agentMetric.setTotalTokens(1650);
            agentMetric.setToolCallCount(2);
            agentMetric.setMutatingToolCallCount(0);
            agentMetric.setFailedToolCallCount(0);
            metricRepository.save(agentMetric);
        };
    }
}
