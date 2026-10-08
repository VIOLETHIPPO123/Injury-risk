import PlayerCard from "../components/PlayerCard";
import RosterLink from "../components/RosterLink";
import { createFormation, createDefenseFormation } from "../utils/formation";
import "./TeamRosterPage.css";

function TeamRosterPage({
  team,
  players,
  onBack,
  onPlayerClick,
  watchlistedIds = new Set(),
  onAddToWatchlist,
  unit = "defense", // offense, defense or special-teams
}) {
  const teamPlayers = players.filter((p) => p.team === team);
  const formation =
    unit === "defense"
      ? createDefenseFormation(teamPlayers)
      : createFormation(teamPlayers);

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

      <div className="player-grid">
        {renderRow(formation.topRow, "top-row")}
        {renderRow(formation.middleRow, "middle-row")}
        {renderRow(formation.bottomRow, "bottom-row")}
      </div>
    </section>
  );
}

export default TeamRosterPage;
