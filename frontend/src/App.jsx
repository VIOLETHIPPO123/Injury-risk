import { useEffect, useState } from "react";
import NavMenu from "./components/NavMenu";
import RostersPage from "./pages/RostersPage";
import TeamRosterPage from "./pages/TeamRosterPage";
import SearchPage from "./pages/SearchPage";
import PlayerPage from "./pages/PlayerPage";
import AboutPage from "./pages/AboutPage";
import { getPlayers } from "./services/playerService";
import "./App.css";
import Header from "./components/Header";

function App() {
  const [view, setView] = useState("home");
  const [players, setPlayers] = useState([]);
  const [selectedPlayer, setSelectedPlayer] = useState(null);
  const [selectedTeam, setSelectedTeam] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    // StrictMode runs effects twice in dev — ignore the result of the discarded run.
    let ignore = false;

    getPlayers()
      .then((data) => {
        if (!ignore) setPlayers(data);
      })
      .catch((err) => {
        if (!ignore) setError(err.message);
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, []);

  let content;
  if (loading) {
    content = <p>Loading players…</p>;
  } else if (error) {
    content = <p>Couldn't load players: {error}</p>;
  } else if (view === "rosters") {
    content = (
      <RostersPage
        players={players}
        onBack={() => setView("home")}
        onTeamClick={(team) => {
          setSelectedTeam(team);
          setView("team-roster");
        }}
      />
    );
  } else if (view === "team-roster") {
    content = (
      <TeamRosterPage
        team={selectedTeam}
        players={players}
        onBack={() => setView("rosters")}
        onPlayerClick={(player) => {
          setSelectedPlayer(player);
          setView("player");
        }}
      />
    );
  } else if (view === "search") {
    content = <SearchPage players={players} onBack={() => setView("home")} />;
  } else if (view === "player") {
    content = (
      <PlayerPage
        player={selectedPlayer}
        onBack={() => setView("team-roster")}
      />
    );
  } else if (view === "about") {
    content = <AboutPage onBack={() => setView("home")} />;
  } else {
    content = (
      <NavMenu
        items={[
          { label: "Roster", onClick: () => setView("rosters") },
          { label: "Search", onClick: () => setView("search") },
          { label: "About", onClick: () => setView("about") },
        ]}
      />
    );
  }

  return (
    <main className="app">
      <Header />
      {content}
    </main>
  );
}

export default App;
