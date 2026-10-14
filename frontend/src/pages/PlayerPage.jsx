import { useState } from "react";
import Button from "../components/Button";
import RosterLink from "../components/RosterLink";
import { calculateAcwr, riskLevel } from "../utils/acwr";
import { compareCandidates, positionGroup } from "../utils/positionGroup";
import "./PlayerPage.css";

function ComparePanel({ player, watchlistedPlayers, onClose }) {
  const group = positionGroup(player.position);
  const candidates = compareCandidates(player, watchlistedPlayers);

  return (
    <aside className="compare-panel" aria-label="Compare with watchlist">
      <div className="compare-panel-header">
        <h2>Compare {group}</h2>
        <Button onClick={onClose}>Close</Button>
      </div>

      {candidates.length > 0 ? (
        <ul className="compare-panel-list">
          {candidates.map((other) => {
            const acwr = calculateAcwr(
              other.snapsLastGame,
              other.snapsLast4Games,
            );
            const risk = riskLevel(acwr);
            return (
              <li key={other.id} className={`risk-${risk.tier}`}>
                <span className="compare-panel-name">{other.name}</span>
                <span className="compare-panel-meta">
                  {other.team} · {other.position}
                </span>
                <span className="compare-panel-acwr">
                  ACWR {acwr === null ? "—" : acwr.toFixed(2)} · {risk.label}
                </span>
              </li>
            );
          })}
        </ul>
      ) : (
        <p className="compare-panel-empty">
          No other {group} players on your watchlist.
        </p>
      )}
    </aside>
  );
}

function PlayerPage({ player, watchlistedPlayers = [], onBack }) {
  const [compareOpen, setCompareOpen] = useState(false);

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
        <div className="player-page-compare">
          <Button
            aria-expanded={compareOpen}
            onClick={() => setCompareOpen(true)}
          >
            Compare
          </Button>
        </div>
      </header>

      {compareOpen && (
        <ComparePanel
          player={player}
          watchlistedPlayers={watchlistedPlayers}
          onClose={() => setCompareOpen(false)}
        />
      )}

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
