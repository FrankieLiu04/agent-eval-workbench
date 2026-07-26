package com.frankliu.agentworkbench.api;

import com.frankliu.agentworkbench.api.dto.BenchmarkJobDtos;
import com.frankliu.agentworkbench.service.BenchmarkJobService;
import com.frankliu.agentworkbench.service.BenchmarkComparisonService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.util.List;

@RestController
@RequestMapping("/api/v1/benchmark-jobs")
public class BenchmarkJobController {

    private static final String WORKER_ID = "X-Netagent-Worker-Id";
    private static final String CLAIM_TOKEN = "X-Netagent-Claim-Token";

    private final BenchmarkJobService service;
    private final BenchmarkComparisonService comparisonService;

    public BenchmarkJobController(BenchmarkJobService service, BenchmarkComparisonService comparisonService) {
        this.service = service;
        this.comparisonService = comparisonService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BenchmarkJobDtos.JobResponse launch(@Valid @RequestBody BenchmarkJobDtos.LaunchRequest request) {
        return service.launch(request);
    }

    @PostMapping("/batches")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BenchmarkJobDtos.BatchLaunchResponse launchBatch(
            @Valid @RequestBody BenchmarkJobDtos.BatchLaunchRequest request
    ) {
        return service.launchBatch(request);
    }

    @GetMapping("/batches/{batchId}/comparison")
    public BenchmarkJobDtos.BatchComparisonResponse compare(@PathVariable String batchId) {
        return comparisonService.compare(batchId);
    }

    @GetMapping
    public List<BenchmarkJobDtos.JobResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public BenchmarkJobDtos.JobResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/cancel")
    public BenchmarkJobDtos.JobResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @PostMapping("/claim")
    public ResponseEntity<BenchmarkJobDtos.ClaimResponse> claim(
            @Valid @RequestBody BenchmarkJobDtos.ClaimRequest request
    ) {
        return service.claim(request.workerId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/heartbeat")
    public BenchmarkJobDtos.WorkerStatusResponse heartbeat(
            @PathVariable Long id,
            @RequestHeader(WORKER_ID) String workerId,
            @RequestHeader(CLAIM_TOKEN) String claimToken
    ) {
        return service.heartbeat(id, workerId, claimToken);
    }

    @PostMapping("/{id}/complete")
    public BenchmarkJobDtos.JobResponse complete(
            @PathVariable Long id,
            @RequestHeader(WORKER_ID) String workerId,
            @RequestHeader(CLAIM_TOKEN) String claimToken,
            @RequestBody JsonNode artifact
    ) {
        return service.complete(id, workerId, claimToken, artifact);
    }

    @PostMapping("/{id}/fail")
    public BenchmarkJobDtos.JobResponse fail(
            @PathVariable Long id,
            @RequestHeader(WORKER_ID) String workerId,
            @RequestHeader(CLAIM_TOKEN) String claimToken,
            @Valid @RequestBody BenchmarkJobDtos.FailureRequest request
    ) {
        return service.fail(id, workerId, claimToken, request);
    }

    @PostMapping("/{id}/cancelled")
    public BenchmarkJobDtos.JobResponse cancelled(
            @PathVariable Long id,
            @RequestHeader(WORKER_ID) String workerId,
            @RequestHeader(CLAIM_TOKEN) String claimToken
    ) {
        return service.cancelled(id, workerId, claimToken);
    }
}
