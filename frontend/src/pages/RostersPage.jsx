import PlayerCard from "../components/PlayerCard";
import RosterLink from "../components/RosterLink";
import "./RostersPage.css";

function groupByTeam(players) {
  const byTeam = new Map();

  for (const player of players) {
    if (!byTeam.has(player.team)) byTeam.set(player.team, []);
    byTeam.get(player.team).push(player);
  }

  return [...byTeam.entries()].sort(([teamA], [teamB]) =>
    teamA.localeCompare(teamB),
  );
}

function createFormation(teamPlayers) {
  const byPosition = {};

  for (const player of teamPlayers) {
    if (!byPosition[player.position]) {
      byPosition[player.position] = [];
    }

    byPosition[player.position].push(player);
  }

  return {
    topRow: [
      byPosition.LT?.[0],
      byPosition.LG?.[0],
      byPosition.C?.[0],
      byPosition.RG?.[0],
      byPosition.RT?.[0],
      byPosition.TE?.[0],
    ].filter(Boolean),

    bottomRow: [
      byPosition.WR?.[0],
      byPosition.WR?.[1],
      byPosition.RB?.[0],
      byPosition.QB?.[0],
      byPosition.FB?.[0],
      byPosition.WR?.[2],
    ].filter(Boolean),
  };
}

function RostersPage({ players, onBack, onPlayerClick }) {
  const teams = groupByTeam(players);

  return (
    <section className="rosters-page">
      <RosterLink onClick={onBack}>Back to home</RosterLink>
      <h1>Rosters</h1>

      {teams.map(([team, teamPlayers]) => {
        const formation = createFormation(teamPlayers);
        return (
          <div className="team-group" key={team}>
            <h2>{team}</h2>

            <div className="player-grid">
              <div className="formation-row top-row">
                {formation.topRow.map((player) => (
                  <PlayerCard
                    key={player.id}
                    {...player}
                    onClick={() => onPlayerClick(player)}
                  />
                ))}
              </div>

              <div className="formation-row bottom-row">
                {formation.bottomRow.map((player) => (
                  <PlayerCard
                    key={player.id}
                    {...player}
                    onClick={() => onPlayerClick(player)}
                  />
                ))}
              </div>
            </div>
          </div>
        );
      })}
    </section>
  );
}

export default RostersPage;
