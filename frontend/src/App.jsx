import { useEffect, useRef, useState } from "react";
import NavMenu from "./components/NavMenu";
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
import SignInPage from "./pages/SignInPage";
import SignUpPage from "./pages/SignUpPage";
import { getPlayers } from "./services/playerService";
import { setSignedOutHandler, SignedOutError } from "./services/apiFetch";
import { getMyRole } from "./services/authService";
import { refreshPlayerData } from "./services/adminService";
import { clearSession, loadSession } from "./services/session";
import { startGoogleSignIn } from "./services/oauthRedirect";
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

// Views that need a signed-in user. Opening one signed out shows sign-in instead, and then the
// view itself once the user signs in (there's no router, so the guard lives in the view switch).
const PROTECTED_VIEWS = new Set(["my-roster"]);
const AUTH_VIEWS = new Set(["sign-in", "sign-up"]);
const SESSION_EXPIRED = "Your session has expired. Please sign in again.";

// `oauthResult` is what main.jsx found in the URL on this page load: null, the return from Google
// sign-in ({ signedIn, returnTo }, the token already stored), or a failed one ({ error }).
function App({ oauthResult = null }) {
  const [view, setView] = useState(() => {
    if (oauthResult?.signedIn) return oauthResult.returnTo ?? "home";
    return oauthResult?.error ? "sign-in" : "home";
  });
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
  // A stored, unexpired token counts as signed in; the backend has the last word on each request.
  const [signedIn, setSignedIn] = useState(() => loadSession() !== null);
  // From GET /me once signed in: "USER" or "ADMIN".
  const [role, setRole] = useState(null);
  // The admin's "Refresh player data": whether it's running, and how the last one went
  // ({ ok, text }).
  const [refreshing, setRefreshing] = useState(false);
  const [refreshResult, setRefreshResult] = useState(null);
  // Where to go after signing in from the Sign in button or an expired session.
  const [returnTo, setReturnTo] = useState(null);
  // Why the sign-in page is showing, e.g. the session expired.
  const [authNotice, setAuthNotice] = useState(() =>
    oauthResult?.error ? `Google sign-in didn't work: ${oauthResult.error}` : null,
  );

  // The handler below runs outside React's render, so it reads the current view from a ref.
  const viewRef = useRef(view);
  viewRef.current = view;

  // A request turned away for a missing or expired token: sign out and show sign-in, then come
  // back to this view. apiFetch has already cleared the token; nothing goes in the error banner.
  useEffect(() => {
    setSignedOutHandler(() => {
      const current = viewRef.current;
      forgetSignedInUser();
      setAuthNotice(SESSION_EXPIRED);
      if (!AUTH_VIEWS.has(current)) {
        setReturnTo(current);
        setView("sign-in");
      }
    });
    return () => setSignedOutHandler(null);
  }, []);

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

  // Loaded separately so a roster failure doesn't block the rest of the app. Only while signed
  // in: the roster is the user's own (signing out empties it, in forgetSignedInUser).
  useEffect(() => {
    if (!signedIn) return undefined;
    let ignore = false;

    getMyRole()
      .then((myRole) => {
        if (!ignore) setRole(myRole);
      })
      .catch(reportError(() => ignore));

    getRoster()
      .then((data) => {
        if (!ignore) setRoster(data);
      })
      .catch(reportError(() => ignore));

    getRosterSettings()
      .then((settings) => {
        if (!ignore) setRosterFormat(settings.formatId);
      })
      .catch(reportError(() => ignore));

    return () => {
      ignore = true;
    };
  }, [signedIn]);

  // The formats are the same for everyone and public, so they load whether or not anyone is signed in.
  useEffect(() => {
    let ignore = false;

    getRosterFormats()
      .then((formats) => {
        if (!ignore) setRosterFormats(formats.map(toLineupFormat));
      })
      .catch(reportError(() => ignore));

    return () => {
      ignore = true;
    };
  }, []);

  // A catch handler for roster and account calls. A SignedOutError isn't shown: the user has
  // already been sent to sign-in. `isStale` says whether the result is from a discarded effect run.
  function reportError(isStale = () => false) {
    return (err) => {
      if (err instanceof SignedOutError || isStale()) return;
      setRosterError(err.message);
    };
  }

  // Shows sign-in, then comes back to `next` (or the current view) once signed in.
  function goToSignIn(next = view) {
    if (!AUTH_VIEWS.has(next)) setReturnTo(next);
    setAuthNotice(null);
    setView("sign-in");
  }

  function handleSignedIn() {
    setSignedIn(true);
    setAuthNotice(null);
    // A guarded view (e.g. My Roster) is still the current view, so it simply appears.
    if (AUTH_VIEWS.has(view)) setView(returnTo ?? "home");
    setReturnTo(null);
  }

  // The browser leaves for Google and comes back with a page load, returning to the view the user
  // was headed for (a guarded one like My Roster is still the current view).
  function handleGoogleSignIn() {
    startGoogleSignIn(AUTH_VIEWS.has(view) ? returnTo : view);
  }

  function handleSignOut() {
    clearSession();
    forgetSignedInUser();
    if (PROTECTED_VIEWS.has(view)) setView("home");
  }

  // Admin only: the backend reloads players and snap counts from nflverse, then the app loads
  // the players again so the new numbers show without a page reload. The button is only drawn
  // for an admin, but that is a convenience: the backend checks the role on every call.
  async function handleRefreshPlayerData() {
    setRefreshing(true);
    setRefreshResult(null);
    try {
      const { players: synced, workloads } = await refreshPlayerData();
      setPlayers(await getPlayers());
      setRefreshResult({
        ok: true,
        text: `Player data refreshed: ${synced.added} new and ${synced.updated} updated players, `
          + `${workloads.added} new and ${workloads.updated} corrected games.`,
      });
    } catch (err) {
      // An expired session has already sent the user to sign-in.
      if (!(err instanceof SignedOutError)) {
        setRefreshResult({ ok: false, text: `Refresh failed: ${err.message}` });
      }
    } finally {
      setRefreshing(false);
    }
  }

  // Drops everything that belonged to the signed-in user, so the next user starts clean.
  function forgetSignedInUser() {
    setSignedIn(false);
    setRole(null);
    setRefreshResult(null);
    setRoster([]);
    setRosterFormat(DEFAULT_FORMAT);
    setRosterError(null);
  }

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
      .catch(reportError());
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
      .catch(reportError());
  }

  // Each handler updates local state from the backend's response instead of re-fetching the list.
  function handleAddToRoster(playerId) {
    setRosterError(null);
    addToRoster(playerId)
      .then((entry) => setRoster((prev) => [...prev, entry]))
      .catch(reportError());
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
        reportError()(err);
        return false;
      });
  }

  function handleRemoveFromRoster(id) {
    setRosterError(null);
    removeFromRoster(id)
      .then(() => setRoster((prev) => prev.filter((e) => e.id !== id)))
      .catch(reportError());
  }

  // What the player cards need to show the badge or the add button.
  const rosterControls = {
    ids: new Set(roster.map((entry) => entry.playerId)),
    isFull: limit !== undefined && countedTowardLimit >= limit,
    limit,
    // Signed out, the add button leads to sign-in and then back here.
    onAdd: signedIn ? handleAddToRoster : () => goToSignIn(),
  };

  // The guard: a protected view shows sign-in until the user signs in.
  const shownView = PROTECTED_VIEWS.has(view) && !signedIn ? "sign-in" : view;

  let content;
  if (shownView === "sign-in") {
    content = (
      <SignInPage
        notice={authNotice}
        onSignedIn={handleSignedIn}
        onGoogle={handleGoogleSignIn}
        onSwitchToSignUp={() => {
          if (!AUTH_VIEWS.has(view)) setReturnTo(view);
          setView("sign-up");
        }}
      />
    );
  } else if (shownView === "sign-up") {
    content = (
      <SignUpPage
        onSignedIn={handleSignedIn}
        onGoogle={handleGoogleSignIn}
        onSwitchToSignIn={() => setView("sign-in")}
      />
    );
  } else if (loading) {
    content = <p>Loading players…</p>;
  } else if (error) {
    content = <p>Couldn't load players: {error}</p>;
  } else if (shownView === "rosters") {
    content = (
      <RostersPage
        players={players}
        onTeamClick={(team) => {
          setSelectedTeam(team);
          setView("team-roster");
        }}
      />
    );
  } else if (shownView === "team-roster") {
    content = (
      <TeamRosterPage
        team={selectedTeam}
        players={players}
        onBack={() => setView("rosters")}
        onPlayerClick={(player) => openPlayer(player, "team-roster")}
        roster={rosterControls}
      />
    );
  } else if (shownView === "search") {
    content = (
      <SearchPage
        players={players}
        search={search}
        onSearchChange={setSearch}
        onSelectPlayer={(player) => openPlayer(player, "search")}
        roster={rosterControls}
      />
    );
  } else if (shownView === "my-roster") {
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
        onUpdate={handleUpdateRosterEntry}
        onRemove={handleRemoveFromRoster}
      />
    );
  } else if (shownView === "player") {
    content = (
      <PlayerPage
        player={selectedPlayer}
        players={players}
        rosteredPlayers={rosteredPlayers}
        onBack={() => setView(playerOrigin)}
        backLabel={playerOrigin === "search" ? "Back to search" : "Back to team page"}
      />
    );
  } else if (shownView === "about") {
    content = <AboutPage onNavigateToAcwr={() => setView("acwr-info")} />;
  } else if (shownView === "acwr-info") {
    content = <AcwrInfoPage onBack={() => setView("about")} />;
  } else {
    const introDelay = hasLeftHome ? 0 : TITLE_INTRO_MS;
    const teamCount = new Set(players.map((p) => p.team)).size;
    content = (
      <>
        <HomeBackdrop />
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
      <Header isHome={view === "home"} onHome={() => setView("home")} />
      {/* Sub-pages only: the homepage's feature cards already lead everywhere. */}
      {view !== "home" && (
        <NavMenu
          items={[
            { label: "Roster", onClick: () => setView("rosters") },
            { label: "Search", onClick: () => setView("search") },
            { label: "My Roster", onClick: () => setView("my-roster") },
            { label: "About", onClick: () => setView("about") },
          ]}
        />
      )}
      {/* After the menu, which stays right under the header on every sub-page. */}
      <div className="account-bar">
        {signedIn ? (
          <>
            {role === "ADMIN" && (
              <>
                <button type="button" onClick={handleRefreshPlayerData} disabled={refreshing}>
                  {refreshing ? "Refreshing player data…" : "Refresh player data"}
                </button>
                <span className="account-role">Admin</span>
              </>
            )}
            <button type="button" onClick={handleSignOut}>
              Sign out
            </button>
          </>
        ) : (
          !AUTH_VIEWS.has(shownView) && (
            <button type="button" onClick={() => goToSignIn()}>
              Sign in
            </button>
          )
        )}
      </div>
      {refreshResult && (
        <p
          className={refreshResult.ok ? "refresh-result" : "roster-error"}
          role={refreshResult.ok ? "status" : "alert"}
        >
          {refreshResult.text}
        </p>
      )}
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
