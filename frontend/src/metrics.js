export function calculateDuplicateRate(duplicateToolCalls, toolCalls) {
  return duplicateToolCalls != null && toolCalls ? duplicateToolCalls / toolCalls : null;
}

export function formatScore(value) {
  return value == null ? "--" : Number(value).toFixed(2);
}

export function formatPercent(value) {
  return value == null ? "--" : `${(Number(value) * 100).toFixed(1)}%`;
}

export function formatDuration(milliseconds) {
  if (milliseconds == null) return "--";
  return milliseconds < 1000 ? `${milliseconds} ms` : `${(milliseconds / 1000).toFixed(1)} s`;
}

export function formatNumber(value) {
  return value == null ? "--" : Number(value).toLocaleString();
}

export function formatDate(value) {
  if (!value) return "no timestamp";
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
