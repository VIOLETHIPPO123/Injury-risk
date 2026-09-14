import { useEffect, useState } from "react";
import RosterLink from "./components/RosterLink";
import RostersPage from "./pages/RostersPage";
import SearchPage from "./pages/SearchPage";
import { getPlayers } from "./services/playerService";
import "./App.css";
import Header from "./components/Header";

function App() {
  const [view, setView] = useState("home");
  const [players, setPlayers] = useState([]);
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
    content = <RostersPage players={players} onBack={() => setView("home")} />;
  } else if (view === "search") {
    content = <SearchPage players={players} onBack={() => setView("home")} />;
  } else {
    content = (
      <nav className="home-links">
        <RosterLink onClick={() => setView("rosters")}>
          View full roster →
        </RosterLink>

        <RosterLink onClick={() => setView("search")}>
          Search players →
        </RosterLink>
      </nav>
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
