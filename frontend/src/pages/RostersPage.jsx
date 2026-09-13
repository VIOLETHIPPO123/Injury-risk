import PlayerCard from '../components/PlayerCard'
import RosterLink from '../components/RosterLink'
import './RostersPage.css'

function groupByTeam(players) {
  const byTeam = new Map()
  for (const player of players) {
    if (!byTeam.has(player.team)) byTeam.set(player.team, [])
    byTeam.get(player.team).push(player)
  }
  return [...byTeam.entries()].sort(([teamA], [teamB]) => teamA.localeCompare(teamB))
}

function RostersPage({ players, onBack }) {
  const teams = groupByTeam(players)

  return (
    <section className="rosters-page">
      <RosterLink onClick={onBack}>← Back to home</RosterLink>
      <h1>Rosters</h1>

      {teams.map(([team, teamPlayers]) => (
        <div className="team-group" key={team}>
          <h2>{team}</h2>
          <div className="player-grid">
            {teamPlayers.map((player) => (
              <PlayerCard key={player.id} {...player} />
            ))}
          </div>
        </div>
      ))}
    </section>
  )
}

export default RostersPage
