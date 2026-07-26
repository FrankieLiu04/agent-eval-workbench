package com.frankliu.agentworkbench.service;

import com.frankliu.agentworkbench.domain.BenchmarkJob;
import com.frankliu.agentworkbench.domain.BenchmarkJobStatus;
import com.frankliu.agentworkbench.repository.BenchmarkJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class BenchmarkJobMaintenanceService {

    private static final List<BenchmarkJobStatus> ACTIVE_STATUSES = List.of(
            BenchmarkJobStatus.RUNNING, BenchmarkJobStatus.CANCEL_REQUESTED);

    private final BenchmarkJobRepository repository;
    private final int workerLeaseSeconds;

    public BenchmarkJobMaintenanceService(
            BenchmarkJobRepository repository,
            @Value("${app.benchmark.worker-lease-seconds:30}") int workerLeaseSeconds
    ) {
        this.repository = repository;
        this.workerLeaseSeconds = workerLeaseSeconds;
    }

    @Scheduled(fixedDelayString = "${app.benchmark.sweep-ms:5000}")
    @Transactional
    public void sweep() {
        Instant now = Instant.now();
        for (BenchmarkJob job : repository.findByStatusInForUpdate(ACTIVE_STATUSES)) {
            if (job.getStatus() == BenchmarkJobStatus.RUNNING
                    && !job.getStartedAt().plusSeconds(job.getTimeoutSeconds()).isAfter(now)) {
                finish(job, BenchmarkJobStatus.TIMED_OUT, now, "Benchmark job exceeded its timeout");
            } else if (!job.getHeartbeatAt().plusSeconds(workerLeaseSeconds).isAfter(now)) {
                expireLease(job, now);
            }
        }
    }

    private void expireLease(BenchmarkJob job, Instant now) {
        if (job.getStatus() == BenchmarkJobStatus.CANCEL_REQUESTED) {
            finish(job, BenchmarkJobStatus.CANCELLED, now, null);
        } else {
            finish(job, BenchmarkJobStatus.FAILED, now, "Worker lease expired");
        }
    }

    private void finish(BenchmarkJob job, BenchmarkJobStatus status, Instant now, String errorMessage) {
        job.setStatus(status);
        job.setFinishedAt(now);
        job.setErrorMessage(errorMessage);
    }
}
