import { describe, expect, it } from "vitest";
import { acwrTrend } from "./acwrTrend";

// weeklyAcwr-style weeks from ACWR values, starting at week 1; null is a week with no value.
const weeks = (acwrs) => acwrs.map((acwr, i) => ({ week: i + 1, snaps: 50, acwr }));

describe("acwrTrend", () => {
  it("is Rising when ACWR went up by more than 0.1 over the last 3 values", () => {
    expect(acwrTrend(weeks([1.0, 1.05, 1.11]))).toBe("Rising");
  });

  it("is Falling when ACWR went down by more than 0.1", () => {
    expect(acwrTrend(weeks([1.4, 1.3, 1.29]))).toBe("Falling");
  });

  it("is Steady when the change is 0.1 or less, in either direction", () => {
    expect(acwrTrend(weeks([1.0, 1.2, 1.05]))).toBe("Steady");
    expect(acwrTrend(weeks([1.1, 1.0, 1.0]))).toBe("Steady");
  });

  it("treats a change of exactly 0.1 as Steady, despite float rounding", () => {
    // 1.3 - 1.2 is 0.10000000000000009 in floating point.
    expect(acwrTrend(weeks([1.2, 1.25, 1.3]))).toBe("Steady");
  });

  it("compares the values as shown, to 2 decimals", () => {
    // Shown as 1.00 and 1.11: up 0.11.
    expect(acwrTrend(weeks([1.004, 1.05, 1.106]))).toBe("Rising");
  });

  it("only looks at the newest and oldest of the last 3 values", () => {
    // A spike in the middle, back where it started: Steady. The older 0.5 is ignored.
    expect(acwrTrend(weeks([0.5, 1.0, 1.6, 1.0]))).toBe("Steady");
  });

  it("skips weeks with no value, such as a bye", () => {
    // Last 3 values: 1.0 (week 4), 1.1 (week 6), 1.3 (week 7). Week 5 was a bye.
    expect(acwrTrend(weeks([null, null, null, 1.0, null, 1.1, 1.3]))).toBe("Rising");
  });

  it("works with only 2 values", () => {
    expect(acwrTrend(weeks([null, null, null, 1.0, 0.8]))).toBe("Falling");
  });

  it("has no trend with fewer than 2 values", () => {
    expect(acwrTrend(weeks([null, null, null, 1.0]))).toBeNull();
    expect(acwrTrend([])).toBeNull();
  });
});
