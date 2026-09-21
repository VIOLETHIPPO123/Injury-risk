// ACWR (Acute:Chronic Workload Ratio) compares the most recent game's workload
// against the average of the last four. Roughly 1.0 means "business as usual";
// a big spike is the classic soft-tissue injury warning sign.
export function calculateAcwr(snapsLastGame, snapsLast4Games) {
  const chronic = snapsLast4Games / 4;
  if (!chronic) return null;
  return snapsLastGame / chronic;
}

// tier drives both the text color and the card's background tint —
// under-training and slightly-elevated share "medium" because the risk
// doc treats both as cautionary, just from opposite directions.
export function riskLevel(acwr) {
  if (acwr === null) return { label: "Not enough data", tier: "unknown" };
  if (acwr < 0.8) return { label: "Ramping up", tier: "medium" };
  if (acwr <= 1.3) return { label: "Normal workload", tier: "low" };
  if (acwr <= 1.5) return { label: "Slightly elevated", tier: "medium" };
  return { label: "Spiking — high risk", tier: "high" };
}
