import { useState } from "react";
import RosterLink from "../components/RosterLink";
import { calculateAcwr, riskLevel } from "../utils/acwr";
import { benchSuggestions } from "../utils/lineupSuggestions";
import { BENCH, canMoveTo, DEFAULT_FORMAT, IR } from "../utils/rosterFormats";
import "./MyRosterPage.css";

// `moveOptions` lists every slot this player could be moved to, each with `disabled` set
// when their position isn't allowed there.
function MyRosterItem({ entry, player, slot, moveOptions, onMove, onUpdate, onRemove }) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(entry.note ?? "");

  const acwr = calculateAcwr(player.snapsLastGame, player.snapsLast4Games);
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
    <div className={`my-roster-item risk-${risk.tier}`}>
      <div className="my-roster-item-header">
        <h3>{player.name}</h3>
        <div className="my-roster-item-tags">
          <span className="team">{player.team}</span>
          {/* A QB in the QB slot would repeat "QB"; the position only adds something
              for FLEX, Bench, IR, or e.g. an FB in an RB slot. */}
          {player.position !== slot.label && (
            <span className="position">{player.position}</span>
          )}
          <span className="my-roster-item-risk">
            ACWR {acwr === null ? "—" : acwr.toFixed(2)} · {risk.label}
          </span>
        </div>
      </div>

      <label className="my-roster-move">
        Move to
        <select
          aria-label={`Move ${player.name}`}
          value={slot.id}
          onChange={(e) => onMove(player.id, e.target.value)}
        >
          {moveOptions.map((option) => (
            <option key={option.id} value={option.id} disabled={option.disabled}>
              {option.label}
            </option>
          ))}
        </select>
      </label>

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
    </div>
  );
}

// One row of the lineup: the slot's name, then its player or "Open".
function LineupRow({ label, row, renderItem }) {
  return (
    <li className="lineup-slot">
      <span className="slot">{label}</span>
      {row ? renderItem(row) : <p className="lineup-open">Open</p>}
    </li>
  );
}

// The lineup for the chosen format: starting slots, then bench and IR. `formats` are the
// lineup formats (from toLineupFormat) and `formatId` the chosen one's id. `slots` maps each
// player id to the slot they fill (from placeEntries); `limit` is the format's roster size,
// which players on IR don't count toward.
function MyRosterPage({
  entries,
  players,
  formats = [],
  formatId = DEFAULT_FORMAT,
  slots = new Map(),
  limit,
  onFormatChange,
  onMove,
  onBack,
  onUpdate,
  onRemove,
}) {
  // The format the user picked, waiting for them to confirm the move to the bench.
  const [pendingFormat, setPendingFormat] = useState(null);
  const format = formats.find((f) => f.id === formatId);

  // The formats come from the backend; there's no lineup to draw until they arrive.
  if (!format) {
    return (
      <section className="my-roster-page">
        <RosterLink onClick={onBack}>← Back to home</RosterLink>
        <h1>My Roster</h1>
        <p>Loading roster formats…</p>
      </section>
    );
  }

  const rows = entries
    .map((entry) => ({
      entry,
      player: players.find((p) => p.id === entry.playerId),
      slot: slots.get(entry.playerId),
    }))
    .filter((row) => row.player && row.slot);
  const inSlot = (slot) => rows.filter((row) => row.slot === slot);
  const benchRows = inSlot(BENCH);
  const irRows = inSlot(IR);
  const suggestions = benchSuggestions(rows.map((row) => ({ ...row.player, slot: row.slot })));

  function moveOptions(row) {
    const irFull = irRows.length >= format.ir && row.slot !== IR;
    const starterOptions = format.starters.map((s) => {
      const occupant = rows.find((r) => r.slot === s);
      return {
        id: s.id,
        label: `${s.label} — ${occupant ? occupant.player.name : "open"}`,
        disabled: !canMoveTo(format, s.id, row.player.position),
      };
    });
    const options = [
      ...starterOptions,
      { id: BENCH.id, label: BENCH.label, disabled: false },
      { id: IR.id, label: IR.label, disabled: !canMoveTo(format, IR.id, row.player.position, { irFull }) },
    ];
    // The slots they can move to first, then the greyed-out ones, each group in slot order.
    return [...options.filter((o) => !o.disabled), ...options.filter((o) => o.disabled)];
  }

  const renderItem = (row) => (
    <MyRosterItem
      key={row.entry.id}
      entry={row.entry}
      player={row.player}
      slot={row.slot}
      moveOptions={moveOptions(row)}
      onMove={onMove}
      onUpdate={onUpdate}
      onRemove={onRemove}
    />
  );
  // Filled spots first, then "Open" for each spot left, up to the format's count.
  const spots = (filled, count, label) => [
    ...filled.map((row) => <LineupRow key={row.entry.id} label={label} row={row} renderItem={renderItem} />),
    ...Array.from({ length: Math.max(0, count - filled.length) }, (_, i) => (
      <LineupRow key={`open-${i}`} label={label} />
    )),
  ];

  return (
    <section className="my-roster-page">
      <RosterLink onClick={onBack}>← Back to home</RosterLink>
      <h1>My Roster</h1>
      {limit !== undefined && (
        <p className="my-roster-count">
          {entries.filter((e) => e.slot !== IR.slot).length} / {limit} players
        </p>
      )}

      <div className="lineup-format">
        <label htmlFor="lineup-format">Format</label>
        <select
          id="lineup-format"
          value={formatId}
          onChange={(e) => setPendingFormat(e.target.value)}
        >
          {formats.map((f) => (
            <option key={f.id} value={f.id}>
              {f.label}
            </option>
          ))}
        </select>
      </div>

      {pendingFormat && (
        <div className="lineup-confirm" role="alertdialog" aria-label="Change format">
          <p>
            Switching to {formats.find((f) => f.id === pendingFormat).label} moves every player to the
            bench.
          </p>
          <div className="my-roster-item-actions">
            <button
              type="button"
              onClick={() => {
                onFormatChange(pendingFormat);
                setPendingFormat(null);
              }}
            >
              Switch format
            </button>
            <button type="button" onClick={() => setPendingFormat(null)}>
              Cancel
            </button>
          </div>
        </div>
      )}

      {suggestions.length > 0 && (
        <ul className="lineup-suggestions" aria-label="Lineup suggestions">
          {suggestions.map((s) => (
            <li key={s.starter.id}>{s.text}</li>
          ))}
        </ul>
      )}

      {entries.length === 0 && (
        <p className="my-roster-empty">
          Your roster is empty. Use "Add to roster" on any eligible player card.
        </p>
      )}

      <h2>Starters</h2>
      <ul className="my-roster-list" aria-label="Starters">
        {format.starters.map((s) => (
          <LineupRow key={s.id} label={s.label} row={rows.find((r) => r.slot === s)} renderItem={renderItem} />
        ))}
      </ul>

      <h2>Bench</h2>
      <ul className="my-roster-list" aria-label="Bench">
        {spots(benchRows, format.bench, BENCH.label)}
      </ul>

      <h2>IR</h2>
      <ul className="my-roster-list" aria-label="IR">
        {spots(irRows, format.ir, IR.label)}
      </ul>
    </section>
  );
}

export default MyRosterPage;
