import { useMemo, useState } from "react";
import SearchBar from "../components/SearchBar";
import FilterChips from "../components/FilterChips";
import PlayerCard from "../components/PlayerCard";
import RosterLink from "../components/RosterLink";
import { EMPTY_SEARCH, filterPlayers, hasActiveQuery, hasActiveSearch } from "../utils/filterPlayers";
import "./SearchPage.css";

const MAX_SUGGESTIONS = 6;

// `search`/`onSearchChange` let a parent own the query and chips so they survive leaving
// the page; without them the page keeps its own state. `lockedPositions` restricts results
// to those positions and hides the position chips. Picking a suggestion or a result card
// calls onPlayerClick.
function SearchPage({
  players = [],
  onBack,
  watchlistedIds = new Set(),
  onAddToWatchlist,
  search,
  onSearchChange,
  lockedPositions,
  onPlayerClick,
}) {
  const [localSearch, setLocalSearch] = useState(EMPTY_SEARCH);
  const current = search ?? localSearch;
  const setSearch = onSearchChange ?? setLocalSearch;
  const update = (changes) => setSearch({ ...current, ...changes });

  // Positions in the order they appear in the data (offense, defense, special teams).
  const positionOptions = useMemo(
    () => [...new Set(players.map((p) => p.position))],
    [players],
  );
  const teamOptions = useMemo(
    () => [...new Set(players.map((p) => p.team))].sort(),
    [players],
  );

  const pool = lockedPositions
    ? players.filter((p) => lockedPositions.includes(p.position))
    : players;
  // In locked mode the position chips are hidden, so any stored selection is ignored.
  const positions = lockedPositions ? [] : current.positions;
  const effectiveSearch = { ...current, positions };
  const filteredPlayers = filterPlayers(pool, effectiveSearch);

  // Until a query of 2+ characters is submitted or a chip is picked, show a prompt instead of results.
  const searching = hasActiveSearch(effectiveSearch);
  // Suggestions follow what's typed; the results grid waits for Enter.
  const suggestions = hasActiveQuery(current.input)
    ? filterPlayers(pool, { ...effectiveSearch, query: current.input }).slice(0, MAX_SUGGESTIONS)
    : [];
  const isActive =
    current.input !== "" || current.query !== "" || positions.length > 0 || current.teams.length > 0;
  const prompt = lockedPositions
    ? "Search a name or pick a team."
    : "Search a name or pick a position/team.";

  return (
    <section className="search-page">
      <RosterLink onClick={onBack}>← Back to home</RosterLink>
      <h1>Search Players</h1>

      <SearchBar
        value={current.input}
        onSearch={(input) => update({ input })}
        onSubmit={(query) => update({ query })}
        suggestions={suggestions}
        onSelect={onPlayerClick}
      />

      {!lockedPositions && (
        <FilterChips
          label="Position"
          options={positionOptions}
          selected={current.positions}
          onChange={(selected) => update({ positions: selected })}
        />
      )}
      <FilterChips
        label="Team"
        options={teamOptions}
        selected={current.teams}
        onChange={(selected) => update({ teams: selected })}
      />

      <button
        type="button"
        className="search-reset"
        disabled={!isActive}
        onClick={() => setSearch(EMPTY_SEARCH)}
      >
        Reset
      </button>

      {!searching && <p className="search-prompt">{prompt}</p>}
      {searching && filteredPlayers.length === 0 && (
        <p className="no-results">No players found</p>
      )}
      {filteredPlayers.length > 0 && (
        <div className="player-grid">
          {filteredPlayers.map((player) => (
            <PlayerCard
              key={player.id}
              {...player}
              onClick={() => onPlayerClick?.(player)}
              isWatchlisted={watchlistedIds.has(player.id)}
              onAddToWatchlist={onAddToWatchlist}
            />
          ))}
        </div>
      )}
    </section>
  );
}

export default SearchPage;
