import PlayerCard from "../components/PlayerCard";
import RosterLink from "../components/RosterLink";
import {
  createFormation,
  createDefenseFormation,
  createSpecialTeamsFormation,
} from "../utils/formation";
import "./TeamRosterPage.css";

function TeamRosterPage({
  team,
  players,
  onBack,
  onPlayerClick,
  watchlistedIds = new Set(),
  onAddToWatchlist,
  unit = "all", // TEMP: "offense" | "defense" | "special-teams" | "all"
}) {
  const teamPlayers = players.filter((p) => p.team === team);

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

  const renderFormation = (formation, className) => (
    <div className={`player-grid ${className}`}>
      {renderRow(formation.topRow, "top-row")}
      {renderRow(formation.middleRow, "middle-row")}
      {renderRow(formation.bottomRow, "bottom-row")}
    </div>
  );

  return (
    <section className="team-roster-page">
      <RosterLink onClick={onBack}>Back to teams</RosterLink>
      <h1>{team}</h1>

      {(unit === "offense" || unit === "all") &&
        renderFormation(createFormation(teamPlayers), "offense-formation")}

      {(unit === "defense" || unit === "all") &&
        renderFormation(
          createDefenseFormation(teamPlayers),
          "defense-formation",
        )}

      {(unit === "special-teams" || unit === "all") &&
        renderFormation(
          createSpecialTeamsFormation(teamPlayers),
          "special-teams-formation",
        )}
    </section>
  );
}

export default TeamRosterPage;
