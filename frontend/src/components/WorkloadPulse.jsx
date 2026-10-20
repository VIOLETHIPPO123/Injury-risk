import { calculateAcwr, riskLevel } from "../utils/acwr";
import "./WorkloadPulse.css";

// Twelve weeks of a made-up snap count: steady, a caution week, then a spike.
const WEEKLY_LOAD = [38, 44, 41, 47, 43, 50, 46, 52, 49, 55, 66, 88];
const STAGGER_MS = 60;
const LEGEND = [
  { tier: "unknown", label: "Not enough history" },
  { tier: "low", label: "Normal" },
  { tier: "medium", label: "Caution" },
  { tier: "high", label: "High risk" },
];

// Each week is scored the way the app scores a player: that week's load against
// the four weeks before it. The first four weeks have no full history yet.
const WEEKS = WEEKLY_LOAD.map((load, week) => {
  const history = WEEKLY_LOAD.slice(Math.max(0, week - 4), week);
  const acwr =
    history.length === 4
      ? calculateAcwr(load, history.reduce((sum, value) => sum + value, 0))
      : null;
  return { load, acwr, tier: riskLevel(acwr).tier };
});
const PEAK = Math.max(...WEEKLY_LOAD);
const LAST = WEEKS.at(-1);

// An illustration of what the app measures, colored with the same risk tiers as the
// player cards. The bars are decoration; the caption says what they show.
function WorkloadPulse({ delay = 0 }) {
  return (
    <figure className="workload-pulse" data-testid="workload-pulse">
      <div className="workload-pulse-chart" aria-hidden="true">
        {WEEKS.map(({ load, tier }, week) => (
          <span
            key={week}
            className={`workload-pulse-bar risk-${tier}`}
            style={{
              height: `${(load / PEAK) * 100}%`,
              animationDelay: `${delay + week * STAGGER_MS}ms`,
            }}
          >
            {week === WEEKS.length - 1 && (
              <span className="workload-pulse-value">
                {LAST.acwr.toFixed(2)}
              </span>
            )}
          </span>
        ))}
      </div>

      <ul className="workload-pulse-legend">
        {LEGEND.map(({ tier, label }) => (
          <li key={tier}>
            <span className={`workload-pulse-swatch risk-${tier}`} />
            {label}
          </li>
        ))}
      </ul>

      <figcaption>
        Twelve weeks of snaps, scored like a player: steady, then a spike to
        ACWR {LAST.acwr.toFixed(2)}.
      </figcaption>
    </figure>
  );
}

export default WorkloadPulse;
