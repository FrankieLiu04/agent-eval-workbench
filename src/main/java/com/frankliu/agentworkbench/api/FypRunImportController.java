package com.frankliu.agentworkbench.api;

import com.frankliu.agentworkbench.api.dto.FypRunImportDtos;
import com.frankliu.agentworkbench.service.FypRunImportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/imports")
public class FypRunImportController {

    private final FypRunImportService service;

    public FypRunImportController(FypRunImportService service) {
        this.service = service;
    }

    @PostMapping("/fyp-run-json")
    @ResponseStatus(HttpStatus.CREATED)
    public FypRunImportDtos.Response importRun(@Valid @RequestBody FypRunImportDtos.Request request) {
        return service.importRun(request);
    }
}
