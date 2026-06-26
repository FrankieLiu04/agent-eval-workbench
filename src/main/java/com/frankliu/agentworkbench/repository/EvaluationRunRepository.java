package com.frankliu.agentworkbench.repository;

import com.frankliu.agentworkbench.domain.EvaluationRun;
import com.frankliu.agentworkbench.domain.RunSource;
import com.frankliu.agentworkbench.domain.RunStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, Long> {

    @EntityGraph(attributePaths = {"experiment", "agentConfig"})
    List<EvaluationRun> findByExperimentId(Long experimentId);

    @EntityGraph(attributePaths = {"experiment", "agentConfig"})
    List<EvaluationRun> findByStatus(RunStatus status);

    @EntityGraph(attributePaths = {"experiment", "agentConfig"})
    List<EvaluationRun> findBySource(RunSource source);

    @EntityGraph(attributePaths = {"experiment", "agentConfig"})
    List<EvaluationRun> findByExperimentIdAndStatus(Long experimentId, RunStatus status);

    @Override
    @EntityGraph(attributePaths = {"experiment", "agentConfig"})
    List<EvaluationRun> findAll();

    @Override
    @EntityGraph(attributePaths = {"experiment", "agentConfig"})
    Optional<EvaluationRun> findById(Long id);
}
