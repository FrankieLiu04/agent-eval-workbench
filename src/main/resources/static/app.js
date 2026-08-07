const state = {
  runs: [],
  selectedId: null,
  currentBatchId: null,
  batchPollTimer: null,
  batchRequestId: 0,
  followBatch: false,
};

const runList = document.querySelector("#run-list");
const runCount = document.querySelector("#run-count");
const detail = document.querySelector("#run-detail");
const filter = document.querySelector("#run-filter");
const launchForm = document.querySelector("#launch-form");
const launchButton = document.querySelector("#launch-job");
const experimentSelect = document.querySelector("#experiment-select");
const caseSelect = document.querySelector("#case-select");
const configOptions = document.querySelector("#config-options");
const repetitionsInput = document.querySelector("#repetitions-input");
const jobState = document.querySelector("#job-state");

document.querySelector("#refresh-runs").addEventListener("click", loadRuns);
filter.addEventListener("input", renderRunList);
window.addEventListener("hashchange", selectFromHash);
launchForm.addEventListener("submit", launchJob);
jobState.addEventListener("click", event => {
  if (event.target.matches("[data-cancel-job]")) cancelJob(event.target.dataset.cancelJob);
});
configOptions.addEventListener("change", updateLaunchButton);

initialize();

async function initialize() {
  await Promise.all([loadRuns(), loadLaunchContext(), loadLatestJob()]);
}

async function loadLaunchContext() {
  try {
    const [experiments, cases, configs] = await Promise.all([
      fetchJson("/api/v1/experiments"),
      fetchJson("/api/v1/benchmark-cases"),
      fetchJson("/api/v1/agent-configs"),
    ]);
    const activeExperiments = experiments.filter(item => item.status === "ACTIVE");
    fillSelect(experimentSelect, activeExperiments.length ? activeExperiments : experiments,
      item => item.id, item => item.name);
    fillSelect(caseSelect, cases, item => item.caseId,
      item => `${item.title} / ${item.caseId}`);
    renderConfigOptions(configs.filter(item => item.provider !== "OTHER"));
    updateLaunchButton();
  } catch (error) {
    launchButton.disabled = true;
    renderJobError(`Launch options unavailable: ${error.message}`);
  }
}

function renderConfigOptions(configs) {
  configOptions.innerHTML = configs.length ? configs.map(item => {
    const isCurrentMatrix = item.provider === "DEEPSEEK" && (
      (item.modelName === "deepseek-v4-flash" && ["LOW", "HIGH", "MAX"].includes(item.reasoningMode))
      || (item.modelName === "deepseek-v4-pro" && ["HIGH", "MAX"].includes(item.reasoningMode))
    );
    return `
      <label class="profile-option">
        <input type="checkbox" name="agent-config" value="${item.id}" ${isCurrentMatrix ? "checked" : ""}>
        <span><strong>${escapeHtml(item.name)}</strong><small>${escapeHtml(item.modelName)} / ${escapeHtml(item.reasoningMode)}</small></span>
      </label>`;
  }).join("") : '<p class="notice">No runnable profiles are available.</p>';
}

function selectedConfigIds() {
  return [...configOptions.querySelectorAll('input[name="agent-config"]:checked')]
    .map(input => Number(input.value));
}

function updateLaunchButton() {
  launchButton.disabled = !experimentSelect.value || !caseSelect.value || !selectedConfigIds().length;
}

function fillSelect(select, items, value, label) {
  select.innerHTML = items.length
    ? items.map(item => `<option value="${escapeHtml(value(item))}">${escapeHtml(label(item))}</option>`).join("")
    : '<option value="">None available</option>';
}

async function loadLatestJob() {
  try {
    const jobs = await fetchJson("/api/v1/benchmark-jobs");
    if (!jobs.length || state.currentBatchId) return;
    const active = jobs.find(job => !isTerminal(job.status));
    const latest = active || jobs[0];
    if (latest.batchId) await showBatch(latest.batchId, Boolean(active));
  } catch (error) {
    renderJobError(`Job queue unavailable: ${error.message}`);
  }
}

async function launchJob(event) {
  event.preventDefault();
  launchButton.disabled = true;
  launchButton.textContent = "Queuing...";
  try {
    const batch = await fetchJson("/api/v1/benchmark-jobs/batches", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        experimentId: Number(experimentSelect.value),
        caseId: caseSelect.value,
        agentConfigIds: selectedConfigIds(),
        repetitions: Number(repetitionsInput.value),
      }),
    });
    await showBatch(batch.batchId, true);
  } catch (error) {
    renderJobError(error.message);
  } finally {
    updateLaunchButton();
    launchButton.textContent = "Queue comparison";
  }
}

async function showBatch(batchId, follow) {
  state.currentBatchId = batchId;
  state.followBatch = follow;
  clearTimeout(state.batchPollTimer);
  await refreshBatch(batchId);
}

async function refreshBatch(batchId) {
  const requestId = ++state.batchRequestId;
  try {
    const comparison = await fetchJson(`/api/v1/benchmark-jobs/batches/${batchId}/comparison`);
    if (state.currentBatchId !== batchId || state.batchRequestId !== requestId) return;
    renderBatch(comparison);
    if (comparison.active > 0) {
      state.batchPollTimer = setTimeout(() => refreshBatch(batchId), 1500);
    } else if (state.followBatch) {
      state.followBatch = false;
      await loadRuns();
    }
  } catch (error) {
    if (state.currentBatchId !== batchId || state.batchRequestId !== requestId) return;
    renderJobError(`Could not refresh batch ${batchId}: ${error.message}`);
    state.batchPollTimer = setTimeout(() => refreshBatch(batchId), 3000);
  }
}

async function cancelJob(id) {
  try {
    const job = await fetchJson(`/api/v1/benchmark-jobs/${id}/cancel`, { method: "POST" });
    if (state.currentBatchId !== job.batchId) return;
    clearTimeout(state.batchPollTimer);
    await refreshBatch(job.batchId);
  } catch (error) {
    renderJobError(`Could not cancel job ${id}: ${error.message}`);
  }
}

function renderBatch(batch) {
  const activeJobs = batch.jobs.filter(job => ["QUEUED", "RUNNING"].includes(job.status));
  const profiles = batch.profiles.map(profile => `
    <tr>
      <th><strong>${escapeHtml(profile.agentConfigName)}</strong><small>${escapeHtml(profile.model)} / ${escapeHtml(profile.reasoningMode)}</small></th>
      <td>${profile.requested - profile.queued - profile.running}/${profile.requested}</td>
      <td>${profile.evaluated ? `${profile.passed}/${profile.evaluated} (${formatPercent(profile.passRate)})` : "--"}</td>
      <td>${formatPercent(profile.reliabilityRate)}</td>
      <td>${formatScore(profile.averageScore)}</td>
      <td>${formatNumber(profile.averageTotalTokens)}</td>
      <td>${formatNumber(profile.averageAgentSteps)}</td>
      <td>${formatDuration(profile.averageLatencyMs)}</td>
      <td>${formatPercent(profile.duplicateToolRate)}</td>
    </tr>`).join("");
  jobState.innerHTML = `
    <div class="batch-summary">
      <span class="status ${batch.active ? "running" : "succeeded"}">${batch.active ? "RUNNING" : "TERMINAL"}</span>
      <p><strong>${escapeHtml(batch.caseTitle)}</strong> / schema ${escapeHtml(batch.caseSchemaVersion)} / ${batch.terminal} of ${batch.requested} terminal</p>
    </div>
    <div class="comparison-scroll">
      <table class="comparison-table">
        <thead><tr><th>Profile</th><th>Runs</th><th>Capability pass</th><th>Reliability</th><th>Avg score</th><th>Tokens</th><th>Steps</th><th>Duration</th><th>Duplicate rate</th></tr></thead>
        <tbody>${profiles}</tbody>
      </table>
    </div>
    ${activeJobs.length ? `<div class="active-jobs">${activeJobs.map(job =>
      `<button class="icon-button" type="button" data-cancel-job="${job.id}">Cancel #${job.id} ${escapeHtml(job.reasoningMode)}</button>`).join("")}</div>` : ""}`;
}

function renderJobError(message) {
  jobState.innerHTML = `<p class="error">${escapeHtml(message)}</p>`;
}

function isTerminal(status) {
  return ["SUCCEEDED", "FAILED", "CANCELLED", "TIMED_OUT"].includes(status);
}

async function fetchJson(url, options) {
  const response = await fetch(url, options);
  if (response.ok) return response.status === 204 ? null : response.json();
  let message = `${url} returned ${response.status}`;
  try {
    const body = await response.json();
    if (body.message) message = body.message;
  } catch (_) {}
  throw new Error(message);
}

async function loadRuns() {
  runCount.textContent = "Loading runs...";
  try {
    const response = await fetch("/api/v1/evaluation-runs");
    if (!response.ok) throw new Error(`Run list returned ${response.status}`);
    state.runs = await response.json();
    renderRunList();
    selectFromHash();
  } catch (error) {
    runCount.textContent = "Run archive unavailable";
    runList.innerHTML = `<p class="notice error">${escapeHtml(error.message)}</p>`;
  }
}

function renderRunList() {
  const query = filter.value.trim().toLowerCase();
  const visible = state.runs.filter(({ run }) => [
    run.caseId,
    run.agentModel,
    run.agentProvider,
    run.status,
    run.task,
  ].some(value => value?.toLowerCase().includes(query)));

  runCount.textContent = `${visible.length} of ${state.runs.length} runs`;
  if (!visible.length) {
    runList.innerHTML = '<p class="notice">No runs match this filter.</p>';
    return;
  }

  runList.innerHTML = visible.map(item => {
    const run = item.run;
    const selected = run.id === state.selectedId ? " selected" : "";
    return `
      <button class="run-card${selected}" type="button" data-run-id="${run.id}">
        <div class="run-card-top">
          <span class="status ${run.status.toLowerCase()}">${escapeHtml(run.status)}</span>
          <span class="score">${formatScore(item.score)}</span>
        </div>
        <h3>${escapeHtml(run.caseId || run.task)}</h3>
        <div class="run-card-meta">
          <span>${escapeHtml(run.agentModel || "unrecorded model")}</span>
          <span>${formatNumber(item.agentSteps)} steps</span>
          <span>${formatDuration(item.durationMs)}</span>
          <span>${formatDate(run.startedAt)}</span>
        </div>
      </button>`;
  }).join("");

  runList.querySelectorAll("[data-run-id]").forEach(card => {
    card.addEventListener("click", () => {
      window.location.hash = `run/${card.dataset.runId}`;
    });
  });
}

function selectFromHash() {
  const match = window.location.hash.match(/^#run\/(\d+)$/);
  if (!match) return;
  const id = Number(match[1]);
  if (id === state.selectedId) return;
  state.selectedId = id;
  renderRunList();
  loadDetail(id);
}

async function loadDetail(id) {
  detail.innerHTML = '<p class="notice">Loading run evidence...</p>';
  try {
    const response = await fetch(`/api/v1/evaluation-runs/${id}`);
    if (!response.ok) throw new Error(`Run detail returned ${response.status}`);
    const data = await response.json();
    if (id === state.selectedId) renderDetail(data);
  } catch (error) {
    if (id !== state.selectedId) return;
    detail.innerHTML = `<p class="notice error">${escapeHtml(error.message)}</p>`;
  }
}

function renderDetail(data) {
  const run = data.run;
  const artifact = data.artifact;
  const result = artifact?.result;
  const evaluation = artifact?.evaluation;
  const steps = artifact?.trace?.steps || [];

  detail.innerHTML = `
    <header class="detail-header">
      <p class="eyebrow">${escapeHtml(run.runId || `Workbench run ${run.id}`)}</p>
      <h2>${escapeHtml(run.caseId || run.task)}</h2>
      <div class="detail-meta">
        <span>${escapeHtml(run.agentProvider || "unknown provider")} / ${escapeHtml(run.agentModel || "unknown model")}</span>
        <span>schema ${escapeHtml(run.schemaVersion || "n/a")}</span>
        <span>${escapeHtml(run.taskMode || run.source)}</span>
        <span>${formatDate(run.startedAt)}</span>
      </div>
    </header>

    <div class="metric-grid">
      ${metric("Capability", formatScore(data.score))}
      ${metric("Steps", formatNumber(data.agentSteps))}
      ${metric("Duration", formatDuration(data.durationMs))}
      ${metric("Tokens", formatNumber(data.totalTokens))}
      ${metric("Tool calls", formatNumber(data.toolCalls))}
      ${metric("Duplicate", formatNumber(data.duplicateToolCalls))}
      ${metric("Failed", formatNumber(data.failedToolCalls))}
    </div>

    ${artifact ? `
      ${renderEvaluationLenses(data, evaluation, steps)}
      <section class="detail-section">
        <h3>Final answer</h3>
        <p class="answer">${escapeHtml(result?.final_answer || "No final answer recorded.")}</p>
      </section>
      ${renderChecks(evaluation)}
      ${renderTrace(steps)}
    ` : `
      <section class="detail-section">
        <p class="notice">This legacy run has summary metadata but no Workbench-owned artifact.</p>
      </section>
    `}
  `;
}

function renderEvaluationLenses(data, evaluation, steps) {
  const duplicateRate = data.toolCalls ? data.duplicateToolCalls / data.toolCalls : null;
  const lenses = [
    ["Capability", evaluation ? `${evaluation.passed ? "PASS" : "FAIL"} · score ${formatScore(evaluation.score)}` : "No deterministic evaluation"],
    ["Reliability", `${escapeHtml(data.run.status)} · one observed rollout; use repeated batch rate for model reliability`],
    ["Efficiency", `${formatNumber(data.totalTokens)} tokens · ${formatNumber(data.agentSteps)} steps · ${formatDuration(data.durationMs)} · duplicate ${formatPercent(duplicateRate)}`],
    ["Trajectory", `${steps.length} recorded steps · ${formatNumber(data.toolCalls)} tool calls · ${formatNumber(data.failedToolCalls)} failed`],
  ];
  return `
    <section class="detail-section">
      <p class="eyebrow">Evaluation lenses</p>
      <h3>Capability × reliability × efficiency × trajectory</h3>
      <div class="checks">${lenses.map(([name, value]) => `
        <div class="check">
          <span class="check-pass">VIEW</span>
          <code>${escapeHtml(name)}</code>
          <p>${value}</p>
        </div>`).join("")}</div>
    </section>`;
}

function renderChecks(evaluation) {
  if (!evaluation?.checks?.length) return "";
  const checks = evaluation.checks.map(check => `
    <div class="check">
      <span class="${check.passed ? "check-pass" : "check-fail"}">${check.passed ? "PASS" : "FAIL"}</span>
      <code>${escapeHtml(check.name)}</code>
      <p>${escapeHtml(check.detail)}</p>
    </div>`).join("");
  return `
    <section class="detail-section">
      <p class="eyebrow">${escapeHtml(evaluation.evaluator)} evaluator</p>
      <h3>Capability evidence</h3>
      <div class="checks">${checks}</div>
    </section>`;
}

function renderTrace(steps) {
  if (!steps.length) return "";
  return `
    <section class="detail-section">
      <p class="eyebrow">Agent trajectory</p>
      <h3>Trace timeline</h3>
      <div class="trace">${steps.map(renderStep).join("")}</div>
    </section>`;
}

function renderStep(step) {
  const results = new Map((step.tool_results || []).map(result => [result.tool_call_id, result]));
  const calls = (step.tool_calls || []).map(call => {
    const result = results.get(call.id);
    return `
      <div class="tool-call">
        <span class="tool-name">${escapeHtml(call.name)}</span>
        <pre>${escapeHtml(JSON.stringify(call.arguments, null, 2))}</pre>
        ${result ? `<pre>${escapeHtml(result.result || result.error || "No output")}</pre>` : ""}
      </div>`;
  }).join("");

  return `
    <article class="trace-step">
      <div class="trace-meta">
        <span>Step ${step.step}</span>
        <span>${escapeHtml(step.finish_reason || "unknown")}</span>
        <span>${formatDuration((step.elapsed_seconds || 0) * 1000)}</span>
      </div>
      ${step.content ? `<p>${escapeHtml(step.content)}</p>` : ""}
      ${calls}
    </article>`;
}

function metric(label, value) {
  return `<div class="metric"><span>${label}</span><strong>${value}</strong></div>`;
}

function formatScore(value) {
  return value == null ? "--" : Number(value).toFixed(2);
}

function formatPercent(value) {
  return value == null ? "--" : `${(Number(value) * 100).toFixed(1)}%`;
}

function formatDuration(milliseconds) {
  if (milliseconds == null) return "--";
  return milliseconds < 1000 ? `${milliseconds} ms` : `${(milliseconds / 1000).toFixed(1)} s`;
}

function formatNumber(value) {
  return value == null ? "--" : Number(value).toLocaleString();
}

function formatDate(value) {
  if (!value) return "no timestamp";
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
