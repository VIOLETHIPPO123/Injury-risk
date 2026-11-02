// How much ACWR has to move over the last 3 values to count as a trend.
export const TREND_THRESHOLD = 0.1;

// One word for where a player's workload is heading, from weeklyAcwr's output.
// Rule: take the last 3 weeks that have an ACWR (byes and the first three games have none,
// so they are skipped) and compare the newest with the oldest of them.
//   - up by more than 0.1:   "Rising"
//   - down by more than 0.1: "Falling"
//   - otherwise:             "Steady" (a change of exactly 0.1 is Steady)
// Values are compared as shown on the page, to 2 decimals, so the word always agrees with
// the numbers. With fewer than 2 values there is no trend: null.
export function acwrTrend(weeks) {
  const values = weeks.map((w) => w.acwr).filter((acwr) => acwr !== null).slice(-3);
  if (values.length < 2) return null;

  // In hundredths, so 0.1 is exactly 10 and float rounding can't tip a tie either way.
  const change = Math.round(values.at(-1) * 100) - Math.round(values[0] * 100);
  const threshold = Math.round(TREND_THRESHOLD * 100);
  if (change > threshold) return "Rising";
  if (change < -threshold) return "Falling";
  return "Steady";
}
