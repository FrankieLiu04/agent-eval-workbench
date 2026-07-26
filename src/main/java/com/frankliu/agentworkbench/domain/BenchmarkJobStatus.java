package com.frankliu.agentworkbench.domain;

public enum BenchmarkJobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCEL_REQUESTED,
    CANCELLED,
    TIMED_OUT
}
