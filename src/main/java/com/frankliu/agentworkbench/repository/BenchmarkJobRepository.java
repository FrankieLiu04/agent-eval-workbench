package com.frankliu.agentworkbench.repository;

import com.frankliu.agentworkbench.domain.BenchmarkJob;
import com.frankliu.agentworkbench.domain.BenchmarkJobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BenchmarkJobRepository extends JpaRepository<BenchmarkJob, Long> {

    @EntityGraph(attributePaths = {"experiment", "benchmarkCase", "agentConfig", "evaluationRun"})
    List<BenchmarkJob> findAllByOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = {"experiment", "benchmarkCase", "agentConfig", "evaluationRun"})
    List<BenchmarkJob> findAllByBatchIdOrderByIdAsc(String batchId);

    @Override
    @EntityGraph(attributePaths = {"experiment", "benchmarkCase", "agentConfig", "evaluationRun"})
    Optional<BenchmarkJob> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BenchmarkJob> findFirstByStatusOrderByCreatedAtAscIdAsc(BenchmarkJobStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from BenchmarkJob job where job.id = :id")
    Optional<BenchmarkJob> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from BenchmarkJob job where job.status in :statuses")
    List<BenchmarkJob> findByStatusInForUpdate(@Param("statuses") Collection<BenchmarkJobStatus> statuses);
}
