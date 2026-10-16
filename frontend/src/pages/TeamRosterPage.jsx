import { useState } from "react";
import PlayerCard from "../components/PlayerCard";
import RosterLink from "../components/RosterLink";
import { createFormation } from "../utils/formation";
import {
  OFFENSE,
  DEFENSE,
  SPECIAL_TEAMS,
} from "../utils/formationDefinitions";
import "./TeamRosterPage.css";

const UNITS = [
  { id: "offense", label: "Offense", formation: OFFENSE },
  { id: "defense", label: "Defense", formation: DEFENSE },
  { id: "special-teams", label: "Special Teams", formation: SPECIAL_TEAMS },
];

function TeamRosterContent({
  team,
  players,
  onBack,
  onPlayerClick,
  watchlistedIds = new Set(),
  onAddToWatchlist,
  unit: initialUnit = "offense",
}) {
  const [selectedUnit, setSelectedUnit] = useState(initialUnit);

  const teamPlayers = players.filter((p) => p.team === team);
  const active = UNITS.find((u) => u.id === selectedUnit) ?? UNITS[0];
  const rows = createFormation(teamPlayers, active.formation);

  // Rows with no players aren't rendered (e.g. a unit missing a whole position group).
  const renderRow = (row) =>
    row.players.length > 0 && (
      <div key={row.name} className={`formation-row ${row.name}`}>
        {row.players.map((player) => (
          <PlayerCard
            key={player.id}
            {...player}
            onClick={() => onPlayerClick(player)}
            isWatchlisted={watchlistedIds.has(player.id)}
            onAddToWatchlist={onAddToWatchlist}
          />
        ))}
      </div>
    );

  return (
    <section className="team-roster-page">
      <RosterLink onClick={onBack}>Back to teams</RosterLink>
      <h1>{team}</h1>

      <div className="unit-toggle" role="group" aria-label="Lineup unit">
        {UNITS.map((u) => (
          <button
            key={u.id}
            type="button"
            className={`unit-toggle-button ${u.id === active.id ? "active" : ""}`}
            aria-pressed={u.id === active.id}
            onClick={() => setSelectedUnit(u.id)}
          >
            {u.label}
          </button>
        ))}
      </div>

      <div className={`player-grid ${active.id}-formation`}>
        {rows.map(renderRow)}
      </div>
    </section>
  );
}

function TeamRosterPage(props) {
  return <TeamRosterContent key={props.team} {...props} />;
}

export default TeamRosterPage;
