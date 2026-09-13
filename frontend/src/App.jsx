import { useState } from "react";
import PlayerCard from "./components/PlayerCard";
import RosterLink from "./components/RosterLink";
import RostersPage from "./pages/RostersPage";
import { players } from "./data/players";
import "./App.css";
import Header from "./components/Header";

function App() {
  const [view, setView] = useState("home");

  return (
    <main className="app">
      <Header />

      {view === "rosters" ? (
        <RostersPage players={players} onBack={() => setView("home")} />
      ) : (
        <>
          <RosterLink onClick={() => setView("rosters")}>
            View full roster →
          </RosterLink>

          <div className="player-grid">
            {players.map((player) => (
              <PlayerCard key={player.id} {...player} />
            ))}
          </div>
        </>
      )}
    </main>
  );
}

export default App;
