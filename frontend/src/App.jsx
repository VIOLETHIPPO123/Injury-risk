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
import MyRosterPage from "./pages/MyRosterPage";
import AboutPage from "./pages/AboutPage";
import AcwrInfoPage from "./pages/AcwrInfoPage";
import { getPlayers } from "./services/playerService";
import {
  getRoster,
  addToRoster,
  updateRosterEntry,
  moveRosterEntry,
  removeFromRoster,
  getRosterFormats,
  getRosterSettings,
  updateRosterSettings,
} from "./services/rosterService";
import { EMPTY_SEARCH } from "./utils/filterPlayers";
import {
  BENCH,
  DEFAULT_FORMAT,
  IR,
  isStarterSlot,
  placeEntries,
  rosterLimit,
  toLineupFormat,
} from "./utils/rosterFormats";
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
  const [roster, setRoster] = useState([]);
  const [rosterError, setRosterError] = useState(null);
  // Every format the league can pick (from the backend), and the id of the one it has.
  const [rosterFormats, setRosterFormats] = useState([]);
  const [rosterFormat, setRosterFormat] = useState(DEFAULT_FORMAT);
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

  // Loaded separately so a roster failure doesn't block the rest of the app.
  useEffect(() => {
    let ignore = false;

    getRoster()
      .then((data) => {
        if (!ignore) setRoster(data);
      })
      .catch((err) => {
        if (!ignore) setRosterError(err.message);
      });

    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    let ignore = false;

    Promise.all([getRosterFormats(), getRosterSettings()])
      .then(([formats, settings]) => {
        if (ignore) return;
        setRosterFormats(formats.map(toLineupFormat));
        setRosterFormat(settings.formatId);
      })
      .catch((err) => {
        if (!ignore) setRosterError(err.message);
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

  // Until the formats load there's no lineup, so no limit and no slots.
  const format = rosterFormats.find((f) => f.id === rosterFormat);
  const limit = format ? rosterLimit(format) : undefined;
  // In the order they were added (entry ids are assigned in that order), so players
  // sharing a slot keep their spots, e.g. the first RB in RB1.
  const entries = [...roster].sort((a, b) => a.id - b.id);
  const rosterPlayers = entries
    .map((entry) => players.find((p) => p.id === entry.playerId))
    .filter(Boolean);
  const slots = format ? placeEntries(format, entries) : new Map();
  const rosteredPlayers = rosterPlayers.map((p) => ({ ...p, slot: slots.get(p.id) }));
  // IR spots are on top of the roster limit, as on the backend.
  const countedTowardLimit = roster.filter((e) => e.slot !== IR.slot).length;

  const replaceEntry = (updated) =>
    setRoster((prev) => prev.map((e) => (e.id === updated.id ? updated : e)));

  // The backend won't move anyone into a filled spot, so whoever holds the chosen starting
  // spot goes to the bench first. The backend has the last word on every move (an IR
  // move needs an Active injury, say); a refusal shows in the error banner.
  function handleMove(playerId, spotId) {
    const target = [...format.starters, BENCH, IR].find((s) => s.id === spotId);
    const entryId = (id) => roster.find((e) => e.playerId === id).id;
    const occupant = isStarterSlot(target)
      ? rosterPlayers.find((p) => p.id !== playerId && slots.get(p.id) === target)
      : undefined;

    setRosterError(null);
    const benchOccupant = occupant
      ? moveRosterEntry(entryId(occupant.id), BENCH.slot).then(replaceEntry)
      : Promise.resolve();
    benchOccupant
      .then(() => moveRosterEntry(entryId(playerId), target.slot))
      .then(replaceEntry)
      .catch((err) => setRosterError(err.message));
  }

  // The backend moves everyone to the bench when the format changes, so mirror that here
  // rather than re-fetching the roster.
  function handleFormatChange(formatId) {
    setRosterError(null);
    updateRosterSettings(formatId)
      .then((settings) => {
        setRosterFormat(settings.formatId);
        setRoster((prev) => prev.map((e) => ({ ...e, slot: BENCH.slot })));
      })
      .catch((err) => setRosterError(err.message));
  }

  // Each handler updates local state from the backend's response instead of re-fetching the list.
  function handleAddToRoster(playerId) {
    setRosterError(null);
    addToRoster(playerId)
      .then((entry) => setRoster((prev) => [...prev, entry]))
      .catch((err) => setRosterError(err.message));
  }

  // Resolves to true on success so the edit form knows whether to close.
  function handleUpdateRosterEntry(id, note) {
    setRosterError(null);
    return updateRosterEntry(id, note)
      .then((updated) => {
        setRoster((prev) => prev.map((e) => (e.id === id ? updated : e)));
        return true;
      })
      .catch((err) => {
        setRosterError(err.message);
        return false;
      });
  }

  function handleRemoveFromRoster(id) {
    setRosterError(null);
    removeFromRoster(id)
      .then(() => setRoster((prev) => prev.filter((e) => e.id !== id)))
      .catch((err) => setRosterError(err.message));
  }

  // What the player cards need to show the badge or the add button.
  const rosterControls = {
    ids: new Set(roster.map((entry) => entry.playerId)),
    isFull: limit !== undefined && countedTowardLimit >= limit,
    limit,
    onAdd: handleAddToRoster,
  };

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
        roster={rosterControls}
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
        roster={rosterControls}
      />
    );
  } else if (view === "my-roster") {
    content = (
      <MyRosterPage
        entries={roster}
        players={players}
        formats={rosterFormats}
        formatId={rosterFormat}
        slots={slots}
        limit={limit}
        onFormatChange={handleFormatChange}
        onMove={handleMove}
        onBack={() => setView("home")}
        onUpdate={handleUpdateRosterEntry}
        onRemove={handleRemoveFromRoster}
      />
    );
  } else if (view === "player") {
    content = (
      <PlayerPage
        player={selectedPlayer}
        players={players}
        rosteredPlayers={rosteredPlayers}
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
              { label: "My Roster", onClick: () => setView("my-roster") },
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
              id: "my-roster",
              label: "My Roster",
              detail: `On your roster: ${countedTowardLimit}${limit === undefined ? "" : ` / ${limit}`}`,
              onClick: () => setView("my-roster"),
            },
            {
              id: "about",
              label: "About",
              detail: "What Sidelined is and how ACWR works",
              onClick: () => setView("about"),
            },
          ]}
        />
      </>
    );
  }

  return (
    <main className="app">
      <Header />
      {rosterError && (
        <p className="roster-error" role="alert">
          Roster error: {rosterError}
        </p>
      )}
      {content}
    </main>
  );
}

export default App;
