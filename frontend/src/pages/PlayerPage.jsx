import { useEffect, useState } from "react";
import AcwrTrendChart from "../components/AcwrTrendChart";
import Button from "../components/Button";
import InjuryHistory from "../components/InjuryHistory";
import RosterLink from "../components/RosterLink";
import { getInjuries } from "../services/injuryService";
import { getWorkload } from "../services/workloadService";
import { calculateAcwr, riskLevel } from "../utils/acwr";
import { compareVerdict, comparisonRows } from "../utils/comparison";
import {
  compareCandidates,
  positionGroup,
  positionsInGroup,
} from "../utils/positionGroup";
import SearchPage from "./SearchPage";
import "./PlayerPage.css";

// Loads one player's data with `load(playerId)`: { data, error }, data null while loading.
// Tagged with the player it belongs to, so a previous player's data never shows.
function usePlayerData(load, playerId) {
  const [state, setState] = useState({ playerId: null, data: null, error: null });

  useEffect(() => {
    if (playerId === undefined) return;
    // Ignore a response that arrives after the page moved on to another player.
    let ignore = false;

    load(playerId)
      .then((data) => {
        if (!ignore) setState({ playerId, data, error: null });
      })
      .catch((err) => {
        if (!ignore) setState({ playerId, data: null, error: err.message });
      });

    return () => {
      ignore = true;
    };
  }, [load, playerId]);

  const current = state.playerId === playerId;
  return { data: current ? state.data : null, error: current ? state.error : null };
}

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

// `rosteredPlayers` are the players on the user's roster, each with the `slot` they fill.
function ComparePanel({ player, players, rosteredPlayers, onClose }) {
  // The chosen player, from the roster list or the fallback search.
  const [compared, setCompared] = useState(null);
  const comparedId = compared?.id;
  const group = positionGroup(player.position);
  const candidates = compareCandidates(player, rosteredPlayers);

  return (
    <aside className="compare-panel" aria-label="Compare with my roster">
      <div className="compare-panel-header">
        <h2>Compare {group}</h2>
        <Button onClick={onClose}>Close</Button>
      </div>

      {compared && (
        <SideBySide
          player={player}
          other={compared}
          onClear={() => setCompared(null)}
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
                  {/* Like My Roster: skip the position when the slot already says it. */}
                  {[other.slot?.label, other.team, other.position]
                    .filter((part, i, parts) => part && (i !== 2 || part !== parts[0]))
                    .join(" · ")}
                </span>
                <span className="compare-panel-acwr">
                  ACWR {acwr === null ? "—" : acwr.toFixed(2)} · {risk.label}
                </span>
                <Button
                  aria-label={`Compare with ${other.name}`}
                  disabled={other.id === comparedId}
                  onClick={() => setCompared(other)}
                >
                  {other.id === comparedId ? "Comparing" : "Compare"}
                </Button>
              </li>
            );
          })}
        </ul>
      ) : (
        <div className="compare-panel-fallback">
          <p className="compare-panel-empty">
            No other {group} players on your roster.
          </p>
          <p>Search for a {group} player to compare against instead.</p>
          <SearchPage
            players={players.filter((other) => other.id !== player.id)}
            lockedPositions={positionsInGroup(group)}
            onSelectPlayer={setCompared}
          />
        </div>
      )}
    </aside>
  );
}

// backLabel names wherever onBack returns to (the search page or the team roster).
function PlayerPage({
  player,
  players = [],
  rosteredPlayers = [],
  onBack,
  backLabel = "Back to team page",
}) {
  const [compareOpen, setCompareOpen] = useState(false);
  const playerId = player?.id;
  const injuries = usePlayerData(getInjuries, playerId);
  const workload = usePlayerData(getWorkload, playerId);

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
          players={players}
          rosteredPlayers={rosteredPlayers}
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

      <AcwrTrendChart games={workload.data} error={workload.error} />

      <InjuryHistory injuries={injuries.data} error={injuries.error} />

      <p className="player-page-note">
        Note: ACWR (Acute:Chronic Workload Ratio) compares the most recent
        game's workload
      </p>
    </section>
  );
}

export default PlayerPage;
