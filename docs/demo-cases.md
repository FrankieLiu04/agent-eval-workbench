# Demo Cases

All demo cases are synthetic and safe to share.

## synthetic-dns-latency-001

Scenario: a synthetic checkout API appears slow from one client segment.

Expected investigation:

- Confirm that latency is segment-specific.
- Compare DNS lookup timing with backend processing time.
- Recommend resolver failover or resolver health checks.

## synthetic-egress-packet-loss-001

Scenario: a synthetic batch worker reports intermittent failures when sending data to a mock external endpoint.

Expected investigation:

- Compare failure windows with synthetic packet-loss metrics.
- Check whether retries recover the failed calls.
- Recommend route inspection and retry budget tuning.

## synthetic-tls-expiry-warning-001

Scenario: a synthetic service monitor warns that a certificate will expire soon.

Expected investigation:

- Identify the affected synthetic endpoint.
- Estimate remaining validity window.
- Recommend renewal and post-renewal verification.
