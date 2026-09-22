import RosterLink from "../components/RosterLink";
import { calculateAcwr, riskLevel } from "../utils/acwr";
import "./PlayerPage.css";

function PlayerPage({ player, onBack }) {
  if (!player) {
    return (
      <section className="player-page">
        <RosterLink onClick={onBack}>Back to team page</RosterLink>
        <h1>No player found.</h1>
      </section>
    );
  }

  const { name, team, position, snapsLastGame, snapsLast4Games } = player;
  const acwr = calculateAcwr(snapsLastGame, snapsLast4Games);
  const risk = riskLevel(acwr);
  const chronicAvg = snapsLast4Games ? snapsLast4Games / 4 : null;

  return (
    <section className={`player-page risk-${risk.tier}`}>
      <RosterLink onClick={onBack}>Back to team page</RosterLink>

      <header className="player-page-header">
        <h1>{name}</h1>
        <div className="player-page-tags">
          <span className="team">{team}</span>
          <span className="position">{position}</span>
        </div>
      </header>

      <div className={`player-page-acwr risk-${risk.tier}`}>
        <span className="player-page-acwr-value">
          {acwr === null ? "—" : acwr.toFixed(2)}
        </span>
        <span className="player-page-acwr-label">{risk.label}</span>
      </div>

      <dl className="player-page-stats">
        <div>
          <dt>Last game</dt>
          <dd>{snapsLastGame}</dd>
        </div>
        <div>
          <dt>Last 4 games</dt>
          <dd>{snapsLast4Games}</dd>
        </div>
        <div>
          <dt>Last 4 games (avg)</dt>
          <dd>{chronicAvg === null ? "—" : chronicAvg.toFixed(2)}</dd>
        </div>
      </dl>

      <p className="player-page-note">
        Note: ACWR (Acute:Chronic Workload Ratio) compares the most recent
        game's workload
      </p>
    </section>
  );
}

export default PlayerPage;
