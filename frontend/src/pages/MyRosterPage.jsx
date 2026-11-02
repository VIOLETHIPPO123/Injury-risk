import { useState } from "react";
import RosterLink from "../components/RosterLink";
import { calculateAcwr, riskLevel } from "../utils/acwr";
import "./MyRosterPage.css";

function MyRosterItem({ entry, player, slot, onUpdate, onRemove }) {
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
    <li className={`my-roster-item risk-${risk.tier}`}>
      <div className="my-roster-item-header">
        <h2>{player ? player.name : `Unknown player #${entry.playerId}`}</h2>
        {player && (
          <div className="my-roster-item-tags">
            {slot && <span className="slot">{slot.label}</span>}
            <span className="team">{player.team}</span>
            {/* A QB in the QB slot would read "QB · ARI · QB"; the position only adds
                something for FLEX, Bench, or e.g. an FB in an RB slot. */}
            {player.position !== slot?.label && (
              <span className="position">{player.position}</span>
            )}
            <span className="my-roster-item-risk">
              ACWR {acwr === null ? "—" : acwr.toFixed(2)} · {risk.label}
            </span>
          </div>
        )}
      </div>

      {editing ? (
        <form className="my-roster-item-form" onSubmit={handleSave}>
          <label htmlFor={`note-${entry.id}`}>Note</label>
          <textarea
            id={`note-${entry.id}`}
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            rows={2}
          />
          <div className="my-roster-item-actions">
            <button type="submit">Save</button>
            <button type="button" onClick={handleCancel}>
              Cancel
            </button>
          </div>
        </form>
      ) : (
        <>
          <p className="my-roster-item-note">
            {entry.note ? entry.note : <em>No note yet.</em>}
          </p>
          <div className="my-roster-item-actions">
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

// `slots` maps each player id to the slot they fill (from assignSlots); `limit` is the
// current format's roster size.
function MyRosterPage({ entries, players, slots = new Map(), limit, onBack, onUpdate, onRemove }) {
  return (
    <section className="my-roster-page">
      <RosterLink onClick={onBack}>← Back to home</RosterLink>
      <h1>My Roster</h1>
      {limit !== undefined && (
        <p className="my-roster-count">
          {entries.length} / {limit} players
        </p>
      )}

      {entries.length === 0 ? (
        <p className="my-roster-empty">
          Your roster is empty. Use "Add to roster" on any eligible player card.
        </p>
      ) : (
        <ul className="my-roster-list">
          {entries.map((entry) => (
            <MyRosterItem
              key={entry.id}
              entry={entry}
              player={players.find((p) => p.id === entry.playerId)}
              slot={slots.get(entry.playerId)}
              onUpdate={onUpdate}
              onRemove={onRemove}
            />
          ))}
        </ul>
      )}
    </section>
  );
}

export default MyRosterPage;
