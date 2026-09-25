import PlayerCard from "../components/PlayerCard";
import RosterLink from "../components/RosterLink";
import { createFormation } from "../utils/formation";
import "./TeamRosterPage.css";

function TeamRosterPage({
  team,
  players,
  onBack,
  onPlayerClick,
  watchlistedIds = new Set(),
  onAddToWatchlist,
}) {
  const teamPlayers = players.filter((p) => p.team === team);
  const formation = createFormation(teamPlayers);

  return (
    <section className="team-roster-page">
      <RosterLink onClick={onBack}>Back to teams</RosterLink>
      <h1>{team}</h1>

      <div className="player-grid">
        <div className="formation-row top-row">
          {formation.topRow.map((player) => (
            <PlayerCard
              key={player.id}
              {...player}
              onClick={() => onPlayerClick(player)}
              isWatchlisted={watchlistedIds.has(player.id)}
              onAddToWatchlist={onAddToWatchlist}
            />
          ))}
        </div>

        <div className="formation-row bottom-row">
          {formation.bottomRow.map((player) => (
            <PlayerCard
              key={player.id}
              {...player}
              onClick={() => onPlayerClick(player)}
              isWatchlisted={watchlistedIds.has(player.id)}
              onAddToWatchlist={onAddToWatchlist}
            />
          ))}
        </div>
      </div>
    </section>
  );
}

export default TeamRosterPage;
