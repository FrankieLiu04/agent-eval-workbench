package com.frankliu.agentworkbench.repository;

import com.frankliu.agentworkbench.domain.BenchmarkCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BenchmarkCaseRepository extends JpaRepository<BenchmarkCase, String> {

    List<BenchmarkCase> findByEnabledTrueOrderByCaseId();

    boolean existsByCaseId(String caseId);
}
