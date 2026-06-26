package com.frankliu.agentworkbench.api;

import com.frankliu.agentworkbench.api.dto.ExperimentDtos;
import com.frankliu.agentworkbench.domain.ExperimentDomain;
import com.frankliu.agentworkbench.domain.ExperimentStatus;
import com.frankliu.agentworkbench.service.ExperimentService;
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
@RequestMapping("/api/v1/experiments")
public class ExperimentController {

    private final ExperimentService service;

    public ExperimentController(ExperimentService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExperimentDtos.Response> list(
            @RequestParam(required = false) ExperimentDomain domain,
            @RequestParam(required = false) ExperimentStatus status
    ) {
        return service.list(domain, status);
    }

    @GetMapping("/{id}")
    public ExperimentDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExperimentDtos.Response create(@Valid @RequestBody ExperimentDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ExperimentDtos.Response update(@PathVariable Long id, @Valid @RequestBody ExperimentDtos.Request request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
