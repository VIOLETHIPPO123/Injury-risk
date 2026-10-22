import { calculateAcwr, riskLevel } from "../utils/acwr";
import "./WorkloadPulse.css";

// Twelve weeks of a made-up snap count: steady, a caution week, then a spike.
const WEEKLY_LOAD = [38, 44, 41, 47, 43, 50, 46, 52, 49, 55, 66, 88];
const STAGGER_MS = 60;

// Each week is scored the way the app scores a player: that week's load against
// the four weeks before it. The first four weeks have no full history yet.
const WEEKS = WEEKLY_LOAD.map((load, week) => {
  const history = WEEKLY_LOAD.slice(Math.max(0, week - 4), week);
  const acwr =
    history.length === 4
      ? calculateAcwr(load, history.reduce((sum, value) => sum + value, 0))
      : null;
  return { load, tier: riskLevel(acwr).tier };
});
const PEAK = Math.max(...WEEKLY_LOAD);

// Decorative chart of what the app measures, colored with the same risk tiers as the
// player cards. Hidden from screen readers and never catches clicks.
// `delay` holds every bar back, e.g. until the title animation has played.
function WorkloadPulse({ delay = 0 }) {
  return (
    <div className="workload-pulse" aria-hidden="true" data-testid="workload-pulse">
      <div className="workload-pulse-chart">
        {WEEKS.map(({ load, tier }, week) => (
          <span
            key={week}
            className={`workload-pulse-bar risk-${tier}`}
            style={{
              height: `${(load / PEAK) * 100}%`,
              animationDelay: `${delay + week * STAGGER_MS}ms`,
            }}
          />
        ))}
      </div>
    </div>
  );
}

export default WorkloadPulse;
