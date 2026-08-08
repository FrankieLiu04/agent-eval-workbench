import { useCallback, useEffect, useMemo, useState } from "react";
import { fetchJson } from "./api.js";
import {
  calculateDuplicateRate,
  formatDate,
  formatDuration,
  formatNumber,
  formatPercent,
  formatScore,
} from "./metrics.js";

const TERMINAL_JOB_STATUSES = new Set(["SUCCEEDED", "FAILED", "CANCELLED", "TIMED_OUT"]);

function selectedRunFromHash() {
  const match = window.location.hash.match(/^#run\/(\d+)$/);
  return match ? Number(match[1]) : null;
}

export default function App() {
  const [runs, setRuns] = useState([]);
  const [runsLoading, setRunsLoading] = useState(true);
  const [runsError, setRunsError] = useState("");
  const [filter, setFilter] = useState("");
  const [selectedId, setSelectedId] = useState(selectedRunFromHash);
  const [runDetail, setRunDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");
  const [launchContext, setLaunchContext] = useState({ experiments: [], cases: [], configs: [] });
  const [contextError, setContextError] = useState("");
  const [batch, setBatch] = useState(null);
  const [batchError, setBatchError] = useState("");
  const [pollRetry, setPollRetry] = useState(0);

  const loadRuns = useCallback(async () => {
    setRunsLoading(true);
    setRunsError("");
    try {
      setRuns(await fetchJson("/api/v1/evaluation-runs"));
    } catch (error) {
      setRunsError(error.message);
    } finally {
      setRunsLoading(false);
    }
  }, []);

  const loadBatch = useCallback(async batchId => {
    try {
      setBatchError("");
      const comparison = await fetchJson(`/api/v1/benchmark-jobs/batches/${batchId}/comparison`);
      setBatch(comparison);
      return comparison;
    } catch (error) {
      setBatchError(error.message);
      return null;
    }
  }, []);

  useEffect(() => {
    loadRuns();
    Promise.all([
      fetchJson("/api/v1/experiments"),
      fetchJson("/api/v1/benchmark-cases"),
      fetchJson("/api/v1/agent-configs"),
    ]).then(([experiments, cases, configs]) => {
      setLaunchContext({
        experiments: experiments.filter(item => item.status === "ACTIVE").length
          ? experiments.filter(item => item.status === "ACTIVE")
          : experiments,
        cases,
        configs: configs.filter(item => item.provider !== "OTHER"),
      });
    }).catch(error => setContextError(error.message));

    fetchJson("/api/v1/benchmark-jobs")
      .then(jobs => {
        if (!jobs.length) return;
        const active = jobs.find(job => !TERMINAL_JOB_STATUSES.has(job.status));
        const latest = active || jobs[0];
        if (latest.batchId) loadBatch(latest.batchId);
      })
      .catch(error => setBatchError(error.message));
  }, [loadBatch, loadRuns]);

  useEffect(() => {
    const onHashChange = () => setSelectedId(selectedRunFromHash());
    window.addEventListener("hashchange", onHashChange);
    return () => window.removeEventListener("hashchange", onHashChange);
  }, []);

  useEffect(() => {
    if (!selectedId) {
      setRunDetail(null);
      return undefined;
    }
    let current = true;
    setDetailLoading(true);
    setDetailError("");
    fetchJson(`/api/v1/evaluation-runs/${selectedId}`)
      .then(data => current && setRunDetail(data))
      .catch(error => current && setDetailError(error.message))
      .finally(() => current && setDetailLoading(false));
    return () => { current = false; };
  }, [selectedId]);

  useEffect(() => {
    if (!batch?.batchId || !batch.active) return undefined;
    const timer = window.setTimeout(async () => {
      const nextBatch = await loadBatch(batch.batchId);
      if (nextBatch && !nextBatch.active) loadRuns();
      if (!nextBatch) setPollRetry(current => current + 1);
    }, batchError ? 3000 : 1500);
    return () => window.clearTimeout(timer);
  }, [batch, batchError, loadBatch, loadRuns, pollRetry]);

  const selectRun = id => {
    window.location.hash = `run/${id}`;
  };

  return (
    <>
      <Masthead />
      <div className="page-shell">
        <LaunchPanel
          context={launchContext}
          contextError={contextError}
          batch={batch}
          batchError={batchError}
          onBatch={async nextBatch => {
            setBatchError("");
            await loadBatch(nextBatch.batchId);
          }}
          onCancel={async id => {
            try {
              setBatchError("");
              const job = await fetchJson(`/api/v1/benchmark-jobs/${id}/cancel`, { method: "POST" });
              await loadBatch(job.batchId);
            } catch (error) {
              setBatchError(error.message);
            }
          }}
        />

        <main className="workspace">
          <RunBrowser
            runs={runs}
            loading={runsLoading}
            error={runsError}
            filter={filter}
            selectedId={selectedId}
            onFilter={setFilter}
            onRefresh={loadRuns}
            onSelect={selectRun}
          />
          <RunDetail data={runDetail} loading={detailLoading} error={detailError} selectedId={selectedId} />
        </main>
      </div>
    </>
  );
}

function Masthead() {
  return (
    <header className="masthead">
      <a className="brand" href="#top" aria-label="Netagent Eval Workbench home">
        <span className="brand-mark" aria-hidden="true"><i /><i /></span>
        <span className="brand-copy"><strong>netagent</strong><small>eval workbench</small></span>
      </a>
      <div className="masthead-meta">
        <span className="product-context">Agent evaluation</span>
        <span className="system-state"><i />Worker online</span>
      </div>
    </header>
  );
}

function LaunchPanel({ context, contextError, batch, batchError, onBatch, onCancel }) {
  const [experimentId, setExperimentId] = useState("");
  const [caseId, setCaseId] = useState("");
  const [repetitions, setRepetitions] = useState(1);
  const [configIds, setConfigIds] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");

  useEffect(() => {
    setExperimentId(current => current || String(context.experiments[0]?.id || ""));
    setCaseId(current => current || context.cases[0]?.caseId || "");
    setConfigIds(current => current.length ? current : context.configs.filter(isDefaultMatrixProfile).map(item => item.id));
  }, [context]);

  const toggleConfig = id => {
    setConfigIds(current => current.includes(id) ? current.filter(value => value !== id) : [...current, id]);
  };

  const submit = async event => {
    event.preventDefault();
    setSubmitting(true);
    setSubmitError("");
    try {
      const nextBatch = await fetchJson("/api/v1/benchmark-jobs/batches", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          experimentId: Number(experimentId),
          caseId,
          agentConfigIds: configIds,
          repetitions: Number(repetitions),
        }),
      });
      await onBatch(nextBatch);
    } catch (error) {
      setSubmitError(error.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="launch-panel" aria-labelledby="launch-title">
      <div className="launch-intro">
        <span className="section-kicker">Run configuration</span>
        <h1 id="launch-title">New comparison</h1>
        <p>Compare model and reasoning profiles on a versioned benchmark case.</p>
      </div>
      <form className="launch-form" onSubmit={submit}>
        <Field label="Experiment">
          <select value={experimentId} onChange={event => setExperimentId(event.target.value)} required>
            {context.experiments.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
          </select>
        </Field>
        <Field label="Case">
          <select value={caseId} onChange={event => setCaseId(event.target.value)} required>
            {context.cases.map(item => <option key={item.caseId} value={item.caseId}>{item.title} / {item.caseId}</option>)}
          </select>
        </Field>
        <Field label="Repetitions">
          <input type="number" min="1" max="20" value={repetitions} onChange={event => setRepetitions(event.target.value)} required />
        </Field>
        <button className="primary-button" type="submit" disabled={submitting || !experimentId || !caseId || !configIds.length}>
          {submitting ? "Queuing…" : "Queue comparison"}
          <span aria-hidden="true">→</span>
        </button>
        <fieldset className="profile-picker">
          <legend>Model + reasoning profiles</legend>
          <div className="profile-options">
            {context.configs.map(item => (
              <label className="profile-option" key={item.id}>
                <input type="checkbox" checked={configIds.includes(item.id)} onChange={() => toggleConfig(item.id)} />
                <span><strong>{item.name}</strong><small>{item.modelName} / {item.reasoningMode}</small></span>
              </label>
            ))}
          </div>
        </fieldset>
      </form>
      <BatchPanel batch={batch} error={contextError || submitError || batchError} onCancel={onCancel} />
    </section>
  );
}

function Field({ label, children }) {
  return <label className="field"><span>{label}</span>{children}</label>;
}

function BatchPanel({ batch, error, onCancel }) {
  if (error) return <div className="batch-panel"><p className="error">{error}</p></div>;
  if (!batch) return <div className="batch-panel is-empty"><span className="muted-dot" />No benchmark batch selected.</div>;

  const activeJobs = batch.jobs.filter(job => ["QUEUED", "RUNNING"].includes(job.status));
  return (
    <div className="batch-panel">
      <div className="batch-heading">
        <StatusBadge status={batch.active ? "RUNNING" : "TERMINAL"} />
        <p><strong>{batch.caseTitle}</strong><span>schema {batch.caseSchemaVersion} · {batch.terminal} of {batch.requested} terminal</span></p>
      </div>
      <div className="comparison-scroll">
        <table className="comparison-table">
          <thead><tr><th>Profile</th><th>Runs</th><th>Capability pass</th><th>Reliability</th><th>Avg score</th><th>Tokens</th><th>Steps</th><th>Duration</th><th>Duplicate</th></tr></thead>
          <tbody>{batch.profiles.map(profile => (
            <tr key={profile.agentConfigId}>
              <th><strong>{profile.agentConfigName}</strong><small>{profile.model} / {profile.reasoningMode}</small></th>
              <td>{profile.requested - profile.queued - profile.running}/{profile.requested}</td>
              <td>{profile.evaluated ? `${profile.passed}/${profile.evaluated} (${formatPercent(profile.passRate)})` : "--"}</td>
              <td>{formatPercent(profile.reliabilityRate)}</td>
              <td>{formatScore(profile.averageScore)}</td>
              <td>{formatNumber(profile.averageTotalTokens)}</td>
              <td>{formatNumber(profile.averageAgentSteps)}</td>
              <td>{formatDuration(profile.averageLatencyMs)}</td>
              <td>{formatPercent(profile.duplicateToolRate)}</td>
            </tr>
          ))}</tbody>
        </table>
      </div>
      {activeJobs.length > 0 && <div className="active-jobs">{activeJobs.map(job => (
        <button className="secondary-button" type="button" key={job.id} onClick={() => onCancel(job.id)}>Cancel #{job.id} {job.reasoningMode}</button>
      ))}</div>}
    </div>
  );
}

function RunBrowser({ runs, loading, error, filter, selectedId, onFilter, onRefresh, onSelect }) {
  const visible = useMemo(() => {
    const query = filter.trim().toLowerCase();
    return runs.filter(({ run }) => [run.caseId, run.agentModel, run.agentProvider, run.status, run.task]
      .some(value => value?.toLowerCase().includes(query)));
  }, [filter, runs]);

  return (
    <aside className="run-browser">
      <div className="section-heading">
        <div><span className="section-kicker">History</span><h2>Runs</h2></div>
        <button className="icon-button" type="button" onClick={onRefresh} aria-label="Refresh runs">↻</button>
      </div>
      <label className="search-field"><span>Filter runs</span><input type="search" value={filter} onChange={event => onFilter(event.target.value)} placeholder="Case, model, status…" /></label>
      <p className="run-count">{loading ? "Loading runs…" : `${visible.length} of ${runs.length} runs`}</p>
      <div className="run-list">
        {error && <p className="notice error">{error}</p>}
        {!loading && !error && !visible.length && <p className="notice">No runs match this filter.</p>}
        {visible.map(item => <RunCard key={item.run.id} item={item} selected={item.run.id === selectedId} onSelect={onSelect} />)}
      </div>
    </aside>
  );
}

function RunCard({ item, selected, onSelect }) {
  const { run } = item;
  return (
    <button className={`run-card${selected ? " selected" : ""}`} type="button" onClick={() => onSelect(run.id)}>
      <div className="run-card-top"><StatusBadge status={run.status} /><span className="score">{formatScore(item.score)}</span></div>
      <h3>{run.caseId || run.task}</h3>
      <div className="run-card-meta"><span>{run.agentModel || "unrecorded model"}</span><span>{formatNumber(item.agentSteps)} steps</span><span>{formatDuration(item.durationMs)}</span><span>{formatDate(run.startedAt)}</span></div>
    </button>
  );
}

function RunDetail({ data, loading, error, selectedId }) {
  if (!selectedId) return <section className="run-detail"><EmptyState /></section>;
  if (loading) return <section className="run-detail"><p className="notice">Loading run evidence…</p></section>;
  if (error) return <section className="run-detail"><p className="notice error">{error}</p></section>;
  if (!data) return <section className="run-detail"><EmptyState /></section>;

  const { run, artifact } = data;
  const result = artifact?.result;
  const evaluation = artifact?.evaluation;
  const steps = artifact?.trace?.steps || [];
  return (
    <section className="run-detail">
      <header className="detail-header">
        <p className="eyebrow">{run.runId || `Workbench run ${run.id}`}</p>
        <h2>{run.caseId || run.task}</h2>
        <div className="detail-meta"><span>{run.agentProvider || "unknown provider"} / {run.agentModel || "unknown model"}</span><span>schema {run.schemaVersion || "n/a"}</span><span>{run.taskMode || run.source}</span><span>{formatDate(run.startedAt)}</span></div>
      </header>
      <div className="metric-grid">
        <Metric label="Capability" value={formatScore(data.score)} featured />
        <Metric label="Steps" value={formatNumber(data.agentSteps)} />
        <Metric label="Duration" value={formatDuration(data.durationMs)} />
        <Metric label="Tokens" value={formatNumber(data.totalTokens)} />
        <Metric label="Tool calls" value={formatNumber(data.toolCalls)} />
        <Metric label="Duplicate" value={formatNumber(data.duplicateToolCalls)} />
        <Metric label="Failed" value={formatNumber(data.failedToolCalls)} />
      </div>
      {artifact ? <>
        <EvaluationLenses data={data} evaluation={evaluation} steps={steps} />
        <DetailSection title="Final answer"><p className="answer">{result?.final_answer || "No final answer recorded."}</p></DetailSection>
        <EvaluationChecks evaluation={evaluation} />
        <TraceTimeline steps={steps} />
      </> : <DetailSection><p className="notice">This legacy run has summary metadata but no Workbench-owned artifact.</p></DetailSection>}
    </section>
  );
}

function EmptyState() {
  return <div className="empty-state"><span className="empty-icon">↗</span><h2>Select a run</h2><p>Inspect capability, reliability, efficiency, evaluation evidence, and the full agent trajectory.</p></div>;
}

function Metric({ label, value, featured = false }) {
  return <div className={`metric${featured ? " featured" : ""}`}><span>{label}</span><strong>{value}</strong></div>;
}

function EvaluationLenses({ data, evaluation, steps }) {
  const duplicateRate = calculateDuplicateRate(data.duplicateToolCalls, data.toolCalls);
  const lenses = [
    ["Capability", evaluation ? `${evaluation.passed ? "PASS" : "FAIL"} · score ${formatScore(evaluation.score)}` : "No deterministic evaluation"],
    ["Reliability", `${data.run.status} · one rollout; use repeated batch rate for model reliability`],
    ["Efficiency", `${formatNumber(data.totalTokens)} tokens · ${formatNumber(data.agentSteps)} steps · ${formatDuration(data.durationMs)} · duplicate ${formatPercent(duplicateRate)}`],
    ["Trajectory", `${steps.length} recorded steps · ${formatNumber(data.toolCalls)} tool calls · ${formatNumber(data.failedToolCalls)} failed`],
  ];
  return <DetailSection eyebrow="Evaluation lenses" title="Capability, reliability, efficiency, trajectory"><div className="checks">{lenses.map(([name, value]) => <Check key={name} status="VIEW" name={name} detail={value} />)}</div></DetailSection>;
}

function EvaluationChecks({ evaluation }) {
  if (!evaluation?.checks?.length) return null;
  return <DetailSection eyebrow={`${evaluation.evaluator} evaluator`} title="Capability evidence"><div className="checks">{evaluation.checks.map(check => <Check key={check.name} status={check.passed ? "PASS" : "FAIL"} name={check.name} detail={check.detail} />)}</div></DetailSection>;
}

function Check({ status, name, detail }) {
  return <div className="check"><StatusBadge status={status} /><code>{name}</code><p>{detail}</p></div>;
}

function TraceTimeline({ steps }) {
  if (!steps.length) return null;
  return <DetailSection eyebrow="Agent trajectory" title="Trace timeline"><div className="trace">{steps.map(step => <TraceStep key={step.step} step={step} />)}</div></DetailSection>;
}

function TraceStep({ step }) {
  const results = new Map((step.tool_results || []).map(result => [result.tool_call_id, result]));
  return <article className="trace-step">
    <div className="trace-meta"><span>Step {step.step}</span><span>{step.finish_reason || "unknown"}</span><span>{formatDuration((step.elapsed_seconds || 0) * 1000)}</span></div>
    {step.content && <p>{step.content}</p>}
    {(step.tool_calls || []).map(call => {
      const result = results.get(call.id);
      return <div className="tool-call" key={call.id}><span className="tool-name">{call.name}</span><pre>{JSON.stringify(call.arguments, null, 2)}</pre>{result && <pre>{String(result.result || result.error || "No output")}</pre>}</div>;
    })}
  </article>;
}

function DetailSection({ eyebrow, title, children }) {
  return <section className="detail-section">{eyebrow && <p className="eyebrow">{eyebrow}</p>}{title && <h3>{title}</h3>}{children}</section>;
}

function StatusBadge({ status }) {
  return <span className={`status ${String(status).toLowerCase()}`}>{status}</span>;
}

function isDefaultMatrixProfile(item) {
  return item.provider === "DEEPSEEK" && (
    (item.modelName === "deepseek-v4-flash" && ["LOW", "HIGH", "MAX"].includes(item.reasoningMode))
    || (item.modelName === "deepseek-v4-pro" && ["HIGH", "MAX"].includes(item.reasoningMode))
  );
}
