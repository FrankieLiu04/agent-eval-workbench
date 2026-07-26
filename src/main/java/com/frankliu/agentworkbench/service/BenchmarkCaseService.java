package com.frankliu.agentworkbench.service;

import com.frankliu.agentworkbench.api.dto.BenchmarkCaseDtos;
import com.frankliu.agentworkbench.api.error.NotFoundException;
import com.frankliu.agentworkbench.domain.BenchmarkCase;
import com.frankliu.agentworkbench.repository.BenchmarkCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BenchmarkCaseService {

    private final BenchmarkCaseRepository repository;

    public BenchmarkCaseService(BenchmarkCaseRepository repository) {
        this.repository = repository;
    }

    public List<BenchmarkCaseDtos.Response> listEnabled() {
        return repository.findByEnabledTrueOrderByCaseId().stream()
                .map(item -> new BenchmarkCaseDtos.Response(
                        item.getCaseId(), item.getTitle(), item.getSchemaVersion()))
                .toList();
    }

    public BenchmarkCase getEntity(String caseId) {
        return repository.findById(caseId)
                .orElseThrow(() -> new NotFoundException("Benchmark case not found: " + caseId));
    }
}
