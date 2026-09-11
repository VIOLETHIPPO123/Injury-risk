import PlayerCard from './components/PlayerCard'
import './App.css'

// Hardcoded for now — these mirror the four players the backend already serves
// from GET /players. DEV-38 replaces this with a real fetch.
const players = [
  { id: 1, name: 'Christian McCaffrey', team: 'SF', snapsLastGame: 58, snapsLast4Games: 212 },
  { id: 2, name: 'Saquon Barkley', team: 'PHI', snapsLastGame: 47, snapsLast4Games: 168 },
  { id: 3, name: "Ja'Marr Chase", team: 'CIN', snapsLastGame: 62, snapsLast4Games: 240 },
  { id: 4, name: 'Nick Chubb', team: 'CLE', snapsLastGame: 31, snapsLast4Games: 96 },
]

function App() {
  return (
    <main className="app">
      <h1>SIDELINED</h1>
      <p className="tagline">Injury risk at a glance</p>

      <div className="player-grid">
        {players.map((player) => (
          <PlayerCard key={player.id} {...player} />
        ))}
      </div>
    </main>
  )
}

export default App
