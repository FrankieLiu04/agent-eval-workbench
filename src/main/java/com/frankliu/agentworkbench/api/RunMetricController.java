package com.frankliu.agentworkbench.api;

import com.frankliu.agentworkbench.api.dto.RunMetricDtos;
import com.frankliu.agentworkbench.service.RunMetricService;
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
@RequestMapping("/api/v1/run-metrics")
public class RunMetricController {

    private final RunMetricService service;

    public RunMetricController(RunMetricService service) {
        this.service = service;
    }

    @GetMapping
    public List<RunMetricDtos.Response> list() {
        return service.list();
    }

    @GetMapping(params = "runId")
    public RunMetricDtos.Response getByRun(@RequestParam Long runId) {
        return service.getByRun(runId);
    }

    @GetMapping("/{id}")
    public RunMetricDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RunMetricDtos.Response create(@Valid @RequestBody RunMetricDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public RunMetricDtos.Response update(@PathVariable Long id, @Valid @RequestBody RunMetricDtos.Request request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
