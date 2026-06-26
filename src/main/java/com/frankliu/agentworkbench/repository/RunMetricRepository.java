package com.frankliu.agentworkbench.repository;

import com.frankliu.agentworkbench.domain.RunMetric;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RunMetricRepository extends JpaRepository<RunMetric, Long> {

    @EntityGraph(attributePaths = {"run", "run.experiment", "run.agentConfig"})
    Optional<RunMetric> findByRunId(Long runId);

    boolean existsByRunId(Long runId);
}
