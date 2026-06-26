package com.frankliu.agentworkbench.api;

import com.frankliu.agentworkbench.api.dto.EvaluationRunDtos;
import com.frankliu.agentworkbench.domain.RunSource;
import com.frankliu.agentworkbench.domain.RunStatus;
import com.frankliu.agentworkbench.service.EvaluationRunService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/evaluation-runs")
public class EvaluationRunController {

    private final EvaluationRunService service;

    public EvaluationRunController(EvaluationRunService service) {
        this.service = service;
    }

    @GetMapping
    public List<EvaluationRunDtos.Response> list(
            @RequestParam(required = false) Long experimentId,
            @RequestParam(required = false) RunStatus status,
            @RequestParam(required = false) RunSource source
    ) {
        return service.list(experimentId, status, source);
    }

    @GetMapping("/{id}")
    public EvaluationRunDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationRunDtos.Response create(@Valid @RequestBody EvaluationRunDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public EvaluationRunDtos.Response update(@PathVariable Long id, @Valid @RequestBody EvaluationRunDtos.Request request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
