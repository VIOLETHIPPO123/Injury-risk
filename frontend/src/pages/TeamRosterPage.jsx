import { useState } from "react";
import PlayerCard from "../components/PlayerCard";
import RosterLink from "../components/RosterLink";
import {
  createFormation,
  createDefenseFormation,
  createSpecialTeamsFormation,
} from "../utils/formation";
import "./TeamRosterPage.css";

const UNITS = [
  { id: "offense", label: "Offense", buildFormation: createFormation },
  { id: "defense", label: "Defense", buildFormation: createDefenseFormation },
  {
    id: "special-teams",
    label: "Special Teams",
    buildFormation: createSpecialTeamsFormation,
  },
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
  const formation = active.buildFormation(teamPlayers);

  const renderRow = (rowPlayers, className) =>
    rowPlayers &&
    rowPlayers.length > 0 && (
      <div className={`formation-row ${className}`}>
        {rowPlayers.map((player) => (
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
        {renderRow(formation.topRow, "top-row")}
        {renderRow(formation.middleRow, "middle-row")}
        {renderRow(formation.bottomRow, "bottom-row")}
      </div>
    </section>
  );
}

function TeamRosterPage(props) {
  return <TeamRosterContent key={props.team} {...props} />;
}

export default TeamRosterPage;
