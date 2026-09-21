import "./PlayerCard.css";
import { calculateAcwr, riskLevel } from "../utils/acwr";

function PlayerCard({
  id,
  name,
  team,
  position,
  snapsLastGame,
  snapsLast4Games,
  onClick,
}) {
  const acwr = calculateAcwr(snapsLastGame, snapsLast4Games);
  const risk = riskLevel(acwr);

  return (
    <article
      className={`player-card risk-${risk.tier}`}
      onClick={onClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onClick();
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
    </article>
  );
}

export default PlayerCard;
