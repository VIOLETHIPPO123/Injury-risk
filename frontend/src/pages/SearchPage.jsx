import { useMemo, useState } from "react";
import SearchBar from "../components/SearchBar";
import FilterChips from "../components/FilterChips";
import PlayerCard from "../components/PlayerCard";
import RosterLink from "../components/RosterLink";
import { EMPTY_SEARCH, filterPlayers } from "../utils/filterPlayers";
import "./SearchPage.css";

// `search`/`onSearchChange` let a parent own the query and chips so they survive leaving
// the page; without them the page keeps its own state. `lockedPositions` restricts results
// to those positions and hides the position chips. `onSelectPlayer` makes each result
// clickable; without `onBack` there is no back link (e.g. when embedded in a panel).
function SearchPage({
  players = [],
  onBack,
  watchlistedIds = new Set(),
  onAddToWatchlist,
  search,
  onSearchChange,
  lockedPositions,
  onSelectPlayer,
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
  const filteredPlayers = filterPlayers(pool, { ...current, positions });

  const hasChips = positions.length > 0 || current.teams.length > 0;
  const isActive = current.query !== "" || hasChips;

  let noResults = `No players match "${current.query}".`;
  if (hasChips) {
    noResults = current.query
      ? `No players match "${current.query}" with the selected filters.`
      : "No players match the selected filters.";
  }

  return (
    <section className="search-page">
      {onBack && <RosterLink onClick={onBack}>← Back to home</RosterLink>}
      <h1>Search Players</h1>

      <SearchBar
        value={current.query}
        onSearch={(query) => update({ query })}
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

      {filteredPlayers.length > 0 ? (
        <div className="player-grid">
          {filteredPlayers.map((player) => (
            <PlayerCard
              key={player.id}
              {...player}
              isWatchlisted={watchlistedIds.has(player.id)}
              onAddToWatchlist={onAddToWatchlist}
              onClick={onSelectPlayer && (() => onSelectPlayer(player))}
            />
          ))}
        </div>
      ) : (
        <p className="no-results">{noResults}</p>
      )}
    </section>
  );
}

export default SearchPage;
