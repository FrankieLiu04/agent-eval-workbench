package com.frankliu.agentworkbench.service;

import com.frankliu.agentworkbench.api.dto.EvaluationResultDtos;
import com.frankliu.agentworkbench.api.error.ConflictException;
import com.frankliu.agentworkbench.api.error.NotFoundException;
import com.frankliu.agentworkbench.domain.EvaluationResult;
import com.frankliu.agentworkbench.domain.EvaluationRun;
import com.frankliu.agentworkbench.repository.EvaluationResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class EvaluationResultService {

    private final EvaluationResultRepository repository;
    private final EvaluationRunService runService;

    public EvaluationResultService(EvaluationResultRepository repository, EvaluationRunService runService) {
        this.repository = repository;
        this.runService = runService;
    }

    @Transactional(readOnly = true)
    public List<EvaluationResultDtos.Response> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EvaluationResultDtos.Response get(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public EvaluationResultDtos.Response getByRun(Long runId) {
        return repository.findByRunId(runId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Evaluation result not found for run: " + runId));
    }

    public EvaluationResultDtos.Response create(EvaluationResultDtos.Request request) {
        if (repository.existsByRunId(request.runId())) {
            throw new ConflictException("Evaluation result already exists for run: " + request.runId());
        }
        EvaluationResult result = new EvaluationResult();
        applyRequest(result, request);
        return toResponse(repository.save(result));
    }

    public EvaluationResultDtos.Response update(Long id, EvaluationResultDtos.Request request) {
        EvaluationResult result = getEntity(id);
        applyRequest(result, request);
        result.setUpdatedAt(Instant.now());
        return toResponse(repository.save(result));
    }

    public void delete(Long id) {
        EvaluationResult result = getEntity(id);
        repository.delete(result);
    }

    private EvaluationResult getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Evaluation result not found: " + id));
    }

    private void applyRequest(EvaluationResult result, EvaluationResultDtos.Request request) {
        EvaluationRun run = runService.getEntity(request.runId());
        result.setRun(run);
        result.setScore(request.score());
        result.setAccuracy(request.accuracy());
        result.setSuccessCount(request.successCount());
        result.setFailCount(request.failCount());
        result.setWrongCount(request.wrongCount());
        result.setFormatErrorCount(request.formatErrorCount());
        result.setSyntaxErrorCount(request.syntaxErrorCount());
        result.setTestErrorCount(request.testErrorCount());
        result.setSummary(request.summary());
    }

    private EvaluationResultDtos.Response toResponse(EvaluationResult result) {
        return new EvaluationResultDtos.Response(
                result.getId(),
                result.getRun().getId(),
                result.getScore(),
                result.getAccuracy(),
                result.getSuccessCount(),
                result.getFailCount(),
                result.getWrongCount(),
                result.getFormatErrorCount(),
                result.getSyntaxErrorCount(),
                result.getTestErrorCount(),
                result.getSummary(),
                result.getCreatedAt(),
                result.getUpdatedAt()
        );
    }
}
