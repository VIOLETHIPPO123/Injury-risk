import { useEffect, useState } from "react";
import NavMenu from "./components/NavMenu";
import FadeIn from "./components/FadeIn";
import WorkloadPulse from "./components/WorkloadPulse";
import FeatureCards from "./components/FeatureCards";
import HomeBackdrop from "./components/HomeBackdrop";
import RostersPage from "./pages/RostersPage";
import TeamRosterPage from "./pages/TeamRosterPage";
import SearchPage from "./pages/SearchPage";
import PlayerPage from "./pages/PlayerPage";
import WatchlistPage from "./pages/WatchlistPage";
import AboutPage from "./pages/AboutPage";
import AcwrInfoPage from "./pages/AcwrInfoPage";
import { getPlayers } from "./services/playerService";
import {
  getWatchlist,
  addToWatchlist,
  updateWatchlistEntry,
  removeFromWatchlist,
} from "./services/watchlistService";
import { EMPTY_SEARCH } from "./utils/filterPlayers";
import "./App.css";
import Header from "./components/Header";

// Roughly when the header's title animation finishes; the homepage bars start rising then.
const TITLE_INTRO_MS = 1300;

function App() {
  const [view, setView] = useState("home");
  // The header animates only once, so after the first visit the bars shouldn't wait for it.
  const [hasLeftHome, setHasLeftHome] = useState(false);
  if (view !== "home" && !hasLeftHome) setHasLeftHome(true);
  const [players, setPlayers] = useState([]);
  const [selectedPlayer, setSelectedPlayer] = useState(null);
  const [selectedTeam, setSelectedTeam] = useState(null);
  // The view PlayerPage's back button returns to: "search" or "team-roster".
  const [playerOrigin, setPlayerOrigin] = useState("team-roster");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [watchlist, setWatchlist] = useState([]);
  const [watchlistError, setWatchlistError] = useState(null);
  // Lives here, not in SearchPage, so the query and chips survive leaving the page.
  const [search, setSearch] = useState(EMPTY_SEARCH);

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

  // Loaded separately so a watchlist failure doesn't block the rest of the app.
  useEffect(() => {
    let ignore = false;

    getWatchlist()
      .then((data) => {
        if (!ignore) setWatchlist(data);
      })
      .catch((err) => {
        if (!ignore) setWatchlistError(err.message);
      });

    return () => {
      ignore = true;
    };
  }, []);

  function openPlayer(player, origin) {
    setSelectedPlayer(player);
    setPlayerOrigin(origin);
    setView("player");
  }

  const watchlistedIds = new Set(watchlist.map((entry) => entry.playerId));
  const watchlistedPlayers = players.filter((p) => watchlistedIds.has(p.id));

  // Each handler updates local state from the backend's response instead of re-fetching the list.
  function handleAddToWatchlist(playerId) {
    setWatchlistError(null);
    addToWatchlist(playerId)
      .then((entry) => setWatchlist((prev) => [...prev, entry]))
      .catch((err) => setWatchlistError(err.message));
  }

  // Resolves to true on success so the edit form knows whether to close.
  function handleUpdateWatchlistEntry(id, note) {
    setWatchlistError(null);
    return updateWatchlistEntry(id, note)
      .then((updated) => {
        setWatchlist((prev) => prev.map((e) => (e.id === id ? updated : e)));
        return true;
      })
      .catch((err) => {
        setWatchlistError(err.message);
        return false;
      });
  }

  function handleRemoveFromWatchlist(id) {
    setWatchlistError(null);
    removeFromWatchlist(id)
      .then(() => setWatchlist((prev) => prev.filter((e) => e.id !== id)))
      .catch((err) => setWatchlistError(err.message));
  }

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
        onPlayerClick={(player) => openPlayer(player, "team-roster")}
        watchlistedIds={watchlistedIds}
        onAddToWatchlist={handleAddToWatchlist}
      />
    );
  } else if (view === "search") {
    content = (
      <SearchPage
        players={players}
        search={search}
        onSearchChange={setSearch}
        onSelectPlayer={(player) => openPlayer(player, "search")}
        onBack={() => setView("home")}
        watchlistedIds={watchlistedIds}
        onAddToWatchlist={handleAddToWatchlist}
      />
    );
  } else if (view === "watchlist") {
    content = (
      <WatchlistPage
        entries={watchlist}
        players={players}
        onBack={() => setView("home")}
        onUpdate={handleUpdateWatchlistEntry}
        onRemove={handleRemoveFromWatchlist}
      />
    );
  } else if (view === "player") {
    content = (
      <PlayerPage
        player={selectedPlayer}
        players={players}
        watchlistedPlayers={watchlistedPlayers}
        onBack={() => setView(playerOrigin)}
        backLabel={playerOrigin === "search" ? "Back to search" : "Back to team page"}
      />
    );
  } else if (view === "about") {
    content = (
      <AboutPage
        onBack={() => setView("home")}
        onNavigateToAcwr={() => setView("acwr-info")}
      />
    );
  } else if (view === "acwr-info") {
    content = <AcwrInfoPage onBack={() => setView("about")} />;
  } else {
    const introDelay = hasLeftHome ? 0 : TITLE_INTRO_MS;
    const teamCount = new Set(players.map((p) => p.team)).size;
    content = (
      <>
        <HomeBackdrop />
        <FadeIn className="home-menu-layer">
          <NavMenu
            items={[
              { label: "Roster", onClick: () => setView("rosters") },
              { label: "Search", onClick: () => setView("search") },
              { label: "Watchlist", onClick: () => setView("watchlist") },
              { label: "About", onClick: () => setView("about") },
            ]}
          />
        </FadeIn>
        <WorkloadPulse delay={introDelay} />
        <FeatureCards
          delay={introDelay + 500}
          items={[
            {
              id: "roster",
              label: "Roster",
              detail: `Browse all ${teamCount} teams`,
              onClick: () => setView("rosters"),
            },
            {
              id: "search",
              label: "Search",
              detail: `Find any of ${players.length} players`,
              onClick: () => setView("search"),
            },
            {
              id: "watchlist",
              label: "Watchlist",
              detail: `On your list: ${watchlist.length}`,
              onClick: () => setView("watchlist"),
            },
          ]}
        />
      </>
    );
  }

  return (
    <main className="app">
      <Header />
      {watchlistError && (
        <p className="watchlist-error" role="alert">
          Watchlist error: {watchlistError}
        </p>
      )}
      {content}
    </main>
  );
}

export default App;
