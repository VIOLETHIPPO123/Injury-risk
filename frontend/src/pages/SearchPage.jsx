import { useMemo, useState } from "react";
import SearchBar from "../components/SearchBar";
import PlayerCard from "../components/PlayerCard";
import RosterLink from "../components/RosterLink";
import "./SearchPage.css";

function SearchPage({ players, onBack }) {
  const [query, setQuery] = useState("");

  const filteredPlayers = useMemo(() => {
    if (!query) return players;
    return players.filter((player) =>
      player.name.toLowerCase().includes(query),
    );
  }, [players, query]);

  return (
    <section className="search-page">
      <RosterLink onClick={onBack}>← Back to home</RosterLink>
      <h1>Search Players</h1>

      <SearchBar onSearch={setQuery} />

      {filteredPlayers.length > 0 ? (
        <div className="player-grid">
          {filteredPlayers.map((player) => (
            <PlayerCard key={player.id} {...player} />
          ))}
        </div>
      ) : (
        <p className="no-results">No players match "{query}".</p>
      )}
    </section>
  );
}

export default SearchPage;
