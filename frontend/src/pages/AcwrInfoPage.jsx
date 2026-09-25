import RosterLink from "../components/RosterLink";
import "./AcwrInfoPage.css";

// Mirrors the tiers in utils/acwr.js's riskLevel() — kept as display copy here
// since this page explains the scale rather than computing it from real data.
const RISK_SCALE = [
  {
    tier: "medium",
    range: "Below 0.8",
    label: "Ramping up",
    detail: "The player is working a lot less than usual right now.",
  },
  {
    tier: "low",
    range: "0.8 to 1.3",
    label: "Normal workload",
    detail: "This game's workload is in line with recent games.",
  },
  {
    tier: "medium",
    range: "1.3 to 1.5",
    label: "Slightly elevated",
    detail: "The player took on noticeably more than their recent average.",
  },
  {
    tier: "high",
    range: "Above 1.5",
    label: "Spiking — high risk",
    detail: "A sudden jump in workload, the biggest known injury red flag.",
  },
];

const DATA_SOURCES = [
  {
    name: "Player and snap-count data (current build)",
    detail:
      "Illustrative placeholder data for development. Real rosters and snap counts have not been wired in yet.",
  },
  {
    name: "nflverse",
    detail:
      "Planned source for real play-by-play and snap-count data once live data is integrated.",
  },
  {
    name: "Public NFL injury reports",
    detail:
      "Planned source for injury history used to validate and refine the risk model.",
  },
];

function AcwrInfoPage({ onBack }) {
  return (
    <section className="acwr-info-page">
      <RosterLink onClick={onBack}>← Back to home</RosterLink>
      <h1>Understanding ACWR</h1>

      <div className="acwr-explainer">
        <p>
          ACWR stands for <strong>Acute:Chronic Workload Ratio</strong>. In
          plain terms, it compares how hard a player worked in their{" "}
          <strong>most recent game</strong> to how hard they've been working{" "}
          <strong>on average over their last four games</strong>.
        </p>
        <p>
          A sudden jump — playing far more snaps than usual after a quieter
          stretch — is one of the clearest early warning signs for soft-tissue
          injuries like hamstring and groin strains. ACWR turns that jump into
          a single number so it's easy to spot at a glance.
        </p>
        <p>
          A score near <strong>1.0</strong> means the player's workload
          hasn't changed much. Scores much higher or lower than that mean
          something changed — either a big workload spike, or the player
          easing back in after time away.
        </p>
      </div>

      <h2>The risk scale</h2>
      <ul className="acwr-scale">
        {RISK_SCALE.map((step) => (
          <li key={step.label} className={`acwr-scale-item risk-${step.tier}`}>
            <span className="acwr-scale-range">{step.range}</span>
            <span className="acwr-scale-label">{step.label}</span>
            <span className="acwr-scale-detail">{step.detail}</span>
          </li>
        ))}
      </ul>

      <h2>Where our data comes from</h2>
      <ul className="data-sources">
        {DATA_SOURCES.map((source) => (
          <li key={source.name}>
            <strong>{source.name}</strong> — {source.detail}
          </li>
        ))}
      </ul>
    </section>
  );
}

export default AcwrInfoPage;
