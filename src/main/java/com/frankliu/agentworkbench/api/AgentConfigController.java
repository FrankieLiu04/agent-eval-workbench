package com.frankliu.agentworkbench.api;

import com.frankliu.agentworkbench.api.dto.AgentConfigDtos;
import com.frankliu.agentworkbench.domain.AgentProvider;
import com.frankliu.agentworkbench.service.AgentConfigService;
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
@RequestMapping("/api/v1/agent-configs")
public class AgentConfigController {

    private final AgentConfigService service;

    public AgentConfigController(AgentConfigService service) {
        this.service = service;
    }

    @GetMapping
    public List<AgentConfigDtos.Response> list(@RequestParam(required = false) AgentProvider provider) {
        return service.list(provider);
    }

    @GetMapping("/{id}")
    public AgentConfigDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentConfigDtos.Response create(@Valid @RequestBody AgentConfigDtos.Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public AgentConfigDtos.Response update(@PathVariable Long id, @Valid @RequestBody AgentConfigDtos.Request request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
