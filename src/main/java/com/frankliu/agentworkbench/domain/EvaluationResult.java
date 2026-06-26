package com.frankliu.agentworkbench.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "evaluation_results")
public class EvaluationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false, unique = true)
    private EvaluationRun run;

    private BigDecimal score;

    private BigDecimal accuracy;

    private Integer successCount;

    private Integer failCount;

    private Integer wrongCount;

    private Integer formatErrorCount;

    private Integer syntaxErrorCount;

    private Integer testErrorCount;

    @Column(length = 2000)
    private String summary;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EvaluationRun getRun() {
        return run;
    }

    public void setRun(EvaluationRun run) {
        this.run = run;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(BigDecimal accuracy) {
        this.accuracy = accuracy;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public Integer getWrongCount() {
        return wrongCount;
    }

    public void setWrongCount(Integer wrongCount) {
        this.wrongCount = wrongCount;
    }

    public Integer getFormatErrorCount() {
        return formatErrorCount;
    }

    public void setFormatErrorCount(Integer formatErrorCount) {
        this.formatErrorCount = formatErrorCount;
    }

    public Integer getSyntaxErrorCount() {
        return syntaxErrorCount;
    }

    public void setSyntaxErrorCount(Integer syntaxErrorCount) {
        this.syntaxErrorCount = syntaxErrorCount;
    }

    public Integer getTestErrorCount() {
        return testErrorCount;
    }

    public void setTestErrorCount(Integer testErrorCount) {
        this.testErrorCount = testErrorCount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
