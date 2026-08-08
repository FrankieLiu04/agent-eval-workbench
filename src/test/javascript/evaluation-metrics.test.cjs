const test = require("node:test");
const assert = require("node:assert/strict");

const { calculateDuplicateRate } = require("../../main/resources/static/evaluation-metrics.js");

test("keeps a missing schema 1.1 duplicate metric unavailable", () => {
  assert.equal(calculateDuplicateRate(null, 4), null);
  assert.equal(calculateDuplicateRate(undefined, 4), null);
});

test("calculates schema 1.2 duplicate rates when both metrics are available", () => {
  assert.equal(calculateDuplicateRate(0, 4), 0);
  assert.equal(calculateDuplicateRate(1, 4), 0.25);
  assert.equal(calculateDuplicateRate(0, 0), null);
});
