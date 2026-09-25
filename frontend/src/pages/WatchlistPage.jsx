import { useState } from "react";
import RosterLink from "../components/RosterLink";
import { calculateAcwr, riskLevel } from "../utils/acwr";
import "./WatchlistPage.css";

function WatchlistItem({ entry, player, onUpdate, onRemove }) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(entry.note ?? "");

  const acwr = player
    ? calculateAcwr(player.snapsLastGame, player.snapsLast4Games)
    : null;
  const risk = riskLevel(acwr);

  function handleSave(e) {
    e.preventDefault();
    // onUpdate resolves to false when the save failed; stay in edit mode so the draft isn't lost.
    onUpdate(entry.id, draft).then((saved) => {
      if (saved) setEditing(false);
    });
  }

  function handleCancel() {
    setDraft(entry.note ?? "");
    setEditing(false);
  }

  return (
    <li className={`watchlist-item risk-${risk.tier}`}>
      <div className="watchlist-item-header">
        <h2>{player ? player.name : `Unknown player #${entry.playerId}`}</h2>
        {player && (
          <div className="watchlist-item-tags">
            <span className="team">{player.team}</span>
            <span className="position">{player.position}</span>
            <span className="watchlist-item-risk">
              ACWR {acwr === null ? "—" : acwr.toFixed(2)} · {risk.label}
            </span>
          </div>
        )}
      </div>

      {editing ? (
        <form className="watchlist-item-form" onSubmit={handleSave}>
          <label htmlFor={`note-${entry.id}`}>Note</label>
          <textarea
            id={`note-${entry.id}`}
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            rows={2}
          />
          <div className="watchlist-item-actions">
            <button type="submit">Save</button>
            <button type="button" onClick={handleCancel}>
              Cancel
            </button>
          </div>
        </form>
      ) : (
        <>
          <p className="watchlist-item-note">
            {entry.note ? entry.note : <em>No note yet.</em>}
          </p>
          <div className="watchlist-item-actions">
            <button type="button" onClick={() => setEditing(true)}>
              Edit
            </button>
            <button type="button" onClick={() => onRemove(entry.id)}>
              Remove
            </button>
          </div>
        </>
      )}
    </li>
  );
}

function WatchlistPage({ entries, players, onBack, onUpdate, onRemove }) {
  return (
    <section className="watchlist-page">
      <RosterLink onClick={onBack}>← Back to home</RosterLink>
      <h1>Watchlist</h1>

      {entries.length === 0 ? (
        <p className="watchlist-empty">
          Your watchlist is empty. Use "Add to Watchlist" on any player card.
        </p>
      ) : (
        <ul className="watchlist">
          {entries.map((entry) => (
            <WatchlistItem
              key={entry.id}
              entry={entry}
              player={players.find((p) => p.id === entry.playerId)}
              onUpdate={onUpdate}
              onRemove={onRemove}
            />
          ))}
        </ul>
      )}
    </section>
  );
}

export default WatchlistPage;
