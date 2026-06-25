# Demo Cases

Language: [English](#english) | [中文](#中文)

## English

All demo cases are synthetic and safe to share.

### synthetic-dns-latency-001

Scenario: a synthetic checkout API appears slow from one client segment.

Expected investigation:

- Confirm that latency is segment-specific.
- Compare DNS lookup timing with backend processing time.
- Recommend resolver failover or resolver health checks.

### synthetic-egress-packet-loss-001

Scenario: a synthetic batch worker reports intermittent failures when sending data to a mock external endpoint.

Expected investigation:

- Compare failure windows with synthetic packet-loss metrics.
- Check whether retries recover the failed calls.
- Recommend route inspection and retry budget tuning.

### synthetic-tls-expiry-warning-001

Scenario: a synthetic service monitor warns that a certificate will expire soon.

Expected investigation:

- Identify the affected synthetic endpoint.
- Estimate remaining validity window.
- Recommend renewal and post-renewal verification.

## 中文

所有 demo cases 都是合成数据，可以安全分享。

### synthetic-dns-latency-001

场景：一个合成 checkout API 从某个 client segment 访问时表现较慢。

预期调查：

- 确认 latency 是否只影响特定 segment。
- 对比 DNS lookup timing 和 backend processing time。
- 建议 resolver failover 或 resolver health checks。

### synthetic-egress-packet-loss-001

场景：一个合成 batch worker 向 mock external endpoint 发送数据时出现间歇性失败。

预期调查：

- 对比 failure windows 和合成 packet-loss metrics。
- 检查 retries 是否能恢复失败调用。
- 建议 route inspection 和 retry budget tuning。

### synthetic-tls-expiry-warning-001

场景：一个合成 service monitor 提示证书即将过期。

预期调查：

- 识别受影响的合成 endpoint。
- 估算剩余 validity window。
- 建议 renewal 和 post-renewal verification。
