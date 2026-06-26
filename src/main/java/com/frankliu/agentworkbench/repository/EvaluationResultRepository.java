package com.frankliu.agentworkbench.repository;

import com.frankliu.agentworkbench.domain.EvaluationResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, Long> {

    @EntityGraph(attributePaths = {"run", "run.experiment", "run.agentConfig"})
    Optional<EvaluationResult> findByRunId(Long runId);

    boolean existsByRunId(Long runId);
}
