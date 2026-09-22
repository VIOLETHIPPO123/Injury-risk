// import PlayerCard from "../components/PlayerCard";

import RosterLink from "../components/RosterLink";
import { DIVISIONS } from "../data/divisions";
import "./RostersPage.css";

// function groupByTeam(players) {
// const byTeam = new Map();

// for (const player of players) {
// if (!byTeam.has(player.team)) byTeam.set(player.team, []);
// byTeam.get(player.team).push(player);
// }

// return [...byTeam.entries()].sort(([teamA], [teamB]) =>
// teamA.localeCompare(teamB),
// );
// }

function RostersPage({ onBack, onTeamClick }) {
  return (
    <section className="rosters-page">
      <RosterLink onClick={onBack}>Back to home</RosterLink>
      <h1>Rosters</h1>

      {Object.entries(DIVISIONS).map(([conference, divisions]) => (
        <div className="conference-group" key={conference}>
          <h2>{conference}</h2>

          {Object.entries(divisions).map(([division, teams]) => (
            <div className="division-group" key={division}>
              <h3>
                {conference} {division}
              </h3>

              <div className="team-buttons">
                {teams.map((team) => (
                  <button
                    key={team}
                    type="button"
                    className="team-button"
                    onClick={() => onTeamClick(team)}
                  >
                    {team}
                  </button>
                ))}
              </div>
            </div>
          ))}
        </div>
      ))}
    </section>
  );
}

export default RostersPage;
