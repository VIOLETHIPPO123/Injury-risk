import { useState } from "react";
import Button from "../components/Button";
import RosterLink from "../components/RosterLink";
import { calculateAcwr, riskLevel } from "../utils/acwr";
import { compareVerdict, comparisonRows } from "../utils/comparison";
import { compareCandidates, positionGroup } from "../utils/positionGroup";
import "./PlayerPage.css";

function SideBySide({ player, other, onClear }) {
  const rows = comparisonRows(player, other);

  return (
    <section className="side-by-side" aria-label="Side-by-side comparison">
      <table>
        <thead>
          <tr>
            <td />
            <th scope="col">{player.name}</th>
            <th scope="col">{other.name}</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.label}>
              <th scope="row">{row.label}</th>
              {row.values.map((value, column) => (
                <td
                  key={column}
                  className={row.riskier === column ? "riskier" : undefined}
                >
                  {value}
                  {row.riskier === column && (
                    <span className="visually-hidden"> (riskier)</span>
                  )}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      <p className="side-by-side-verdict">{compareVerdict(player, other)}</p>
      <Button onClick={onClear}>Clear comparison</Button>
    </section>
  );
}

function ComparePanel({ player, watchlistedPlayers, onClose }) {
  const [comparedId, setComparedId] = useState(null);
  const group = positionGroup(player.position);
  const candidates = compareCandidates(player, watchlistedPlayers);
  // Looked up each render, so a player taken off the watchlist drops out of the comparison.
  const compared = candidates.find((other) => other.id === comparedId);

  return (
    <aside className="compare-panel" aria-label="Compare with watchlist">
      <div className="compare-panel-header">
        <h2>Compare {group}</h2>
        <Button onClick={onClose}>Close</Button>
      </div>

      {compared && (
        <SideBySide
          player={player}
          other={compared}
          onClear={() => setComparedId(null)}
        />
      )}

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
                <Button
                  aria-label={`Compare with ${other.name}`}
                  disabled={other.id === comparedId}
                  onClick={() => setComparedId(other.id)}
                >
                  {other.id === comparedId ? "Comparing" : "Compare"}
                </Button>
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

// backLabel names wherever onBack returns to (the search page or the team roster).
function PlayerPage({
  player,
  watchlistedPlayers = [],
  onBack,
  backLabel = "Back to team page",
}) {
  const [compareOpen, setCompareOpen] = useState(false);

  if (!player) {
    return (
      <section className="player-page">
        <RosterLink onClick={onBack}>{backLabel}</RosterLink>
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
      <RosterLink onClick={onBack}>{backLabel}</RosterLink>

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
