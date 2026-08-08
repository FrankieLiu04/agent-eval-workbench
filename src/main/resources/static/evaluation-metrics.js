function calculateDuplicateRate(duplicateToolCalls, toolCalls) {
  return duplicateToolCalls != null && toolCalls ? duplicateToolCalls / toolCalls : null;
}

if (typeof module !== "undefined" && module.exports) {
  module.exports = { calculateDuplicateRate };
}
