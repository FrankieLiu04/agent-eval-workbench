package com.frankliu.agentworkbench.api;

import com.frankliu.agentworkbench.api.dto.EvaluationResultDtos;
import com.frankliu.agentworkbench.service.EvaluationResultService;
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
@RequestMapping("/api/v1/evaluation-results")
public class EvaluationResultController {

    private final EvaluationResultService service;

    public EvaluationResultController(EvaluationResultService service) {
        this.service = service;
    }

    @GetMapping
    public List<EvaluationResultDtos.Response> list() {
        return service.list();
    }

    @GetMapping(params = "runId")
    public EvaluationResultDtos.Response getByRun(@RequestParam Long runId) {
        return service.getByRun(runId);
    }

    @GetMapping("/{id}")
    public EvaluationResultDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationResultDtos.Response create(@Valid @RequestBody EvaluationResultDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public EvaluationResultDtos.Response update(
            @PathVariable Long id,
            @Valid @RequestBody EvaluationResultDtos.Request request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
