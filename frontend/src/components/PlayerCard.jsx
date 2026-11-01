import "./PlayerCard.css";
import { calculateAcwr, riskLevel } from "../utils/acwr";
import { isRosterEligible } from "../utils/rosterFormats";

// `roster` ({ ids, isFull, limit, onAdd }) turns on the roster controls: an "On my roster"
// badge for rostered players, otherwise an add button for players in eligible positions.
function PlayerCard({
  id,
  name,
  team,
  position,
  snapsLastGame,
  snapsLast4Games,
  onClick,
  roster,
}) {
  const acwr = calculateAcwr(snapsLastGame, snapsLast4Games);
  const risk = riskLevel(acwr);
  const isRostered = roster?.ids.has(id) ?? false;
  const canAdd = roster && !isRostered && isRosterEligible(position);

  return (
    <article
      className={`player-card risk-${risk.tier}`}
      onClick={onClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onClick?.();
        }
      }}
    >
      <header className="player-card-header">
        <h2>{name}</h2>
        <div className="player-card-tags">
          <span className="team">{team}</span>
          <span className="position">{position}</span>
        </div>
      </header>

      <div className={`acwr risk-${risk.tier}`}>
        <span className="acwr-value">
          {acwr === null ? "—" : acwr.toFixed(2)}
        </span>
        <span className="acwr-label">{risk.label}</span>
      </div>

      <dl className="snaps">
        <div>
          <dt>Last game</dt>
          <dd>{snapsLastGame}</dd>
        </div>
        <div>
          <dt>Last 4 games</dt>
          <dd>{snapsLast4Games}</dd>
        </div>
      </dl>

      {isRostered && <span className="roster-badge">On my roster</span>}
      {canAdd && (
        <button
          type="button"
          className="roster-add"
          disabled={roster.isFull}
          // The card itself is clickable, so keep this button's events from opening the player.
          onClick={(e) => {
            e.stopPropagation();
            roster.onAdd(id);
          }}
          onKeyDown={(e) => e.stopPropagation()}
        >
          {roster.isFull ? `Roster full (${roster.limit})` : "Add to roster"}
        </button>
      )}
    </article>
  );
}

export default PlayerCard;
