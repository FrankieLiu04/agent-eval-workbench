package com.frankliu.agentworkbench.api;

import com.frankliu.agentworkbench.api.dto.BenchmarkCaseDtos;
import com.frankliu.agentworkbench.service.BenchmarkCaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/benchmark-cases")
public class BenchmarkCaseController {

    private final BenchmarkCaseService service;

    public BenchmarkCaseController(BenchmarkCaseService service) {
        this.service = service;
    }

    @GetMapping
    public List<BenchmarkCaseDtos.Response> list() {
        return service.listEnabled();
    }
}
