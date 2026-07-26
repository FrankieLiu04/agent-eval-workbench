package com.frankliu.agentworkbench.api;

import com.frankliu.agentworkbench.api.dto.EvaluationRunDtos;
import com.frankliu.agentworkbench.service.NetagentRunImportService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/imports")
public class NetagentRunImportController {

    private final NetagentRunImportService service;

    public NetagentRunImportController(NetagentRunImportService service) {
        this.service = service;
    }

    @PostMapping("/netagent-run-json")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationRunDtos.DetailResponse importRun(
            @RequestParam Long experimentId,
            @RequestParam(required = false) Long agentConfigId,
            @RequestBody JsonNode artifact
    ) {
        return service.importRun(experimentId, agentConfigId, artifact);
    }
}
