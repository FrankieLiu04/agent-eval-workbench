import { describe, expect, it } from "vitest";
import { calculateDuplicateRate, formatDuration, formatScore } from "./metrics.js";

describe("evaluation metric formatting", () => {
  it("calculates duplicate tool-call rate", () => {
    expect(calculateDuplicateRate(1, 4)).toBe(0.25);
    expect(calculateDuplicateRate(0, 4)).toBe(0);
    expect(calculateDuplicateRate(1, 0)).toBeNull();
  });

  it("formats scores and durations for the dashboard", () => {
    expect(formatScore(0.875)).toBe("0.88");
    expect(formatDuration(250)).toBe("250 ms");
    expect(formatDuration(1200)).toBe("1.2 s");
  });
});
