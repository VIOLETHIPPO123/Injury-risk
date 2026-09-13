import { useMemo, useState } from "react";
import SearchBar from "../components/SearchBar";
import PlayerCard from "../components/PlayerCard";
import "./SearchPage.css";

// Duplicated from App.jsx for now - same four players GET /players serves.
// Worth extracting to a shared src/data/players.js once there's a second
// consumer, so App.jsx and SearchPage.jsx don't drift.
const players = [
  {
    id: 1,
    name: "Christian McCaffrey",
    team: "SF",
    snapsLastGame: 58,
    snapsLast4Games: 212,
  },
  {
    id: 2,
    name: "Saquon Barkley",
    team: "PHI",
    snapsLastGame: 47,
    snapsLast4Games: 168,
  },
  {
    id: 3,
    name: "Ja'Marr Chase",
    team: "CIN",
    snapsLastGame: 62,
    snapsLast4Games: 240,
  },
  {
    id: 4,
    name: "Nick Chubb",
    team: "CLE",
    snapsLastGame: 31,
    snapsLast4Games: 96,
  },
];

function SearchPage() {
  const [query, setQuery] = useState("");

  const filteredPlayers = useMemo(() => {
    if (!query) return players;
    return players.filter((player) =>
      player.name.toLowerCase().includes(query),
    );
  }, [query]);

  return (
    <main className="search-page">
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
    </main>
  );
}

export default SearchPage;
