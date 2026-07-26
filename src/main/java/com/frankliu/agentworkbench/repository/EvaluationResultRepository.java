package com.frankliu.agentworkbench.repository;

import com.frankliu.agentworkbench.domain.EvaluationResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, Long> {

    @EntityGraph(attributePaths = {"run", "run.experiment", "run.agentConfig"})
    Optional<EvaluationResult> findByRunId(Long runId);

    boolean existsByRunId(Long runId);

    List<EvaluationResult> findAllByRunIdIn(List<Long> runIds);
}
