import { useState } from "react";
import "./SearchBar.css";

// Controlled input with local state. Calls onSearch with the trimmed,
// lowercased query on every keystroke so parent components can filter freely.
function SearchBar({ onSearch, placeholder = "Search players…" }) {
  const [query, setQuery] = useState("");

  function handleChange(event) {
    const value = event.target.value;
    setQuery(value);
    onSearch(value.trim().toLowerCase());
  }

  return (
    <input
      type="text"
      className="search-bar"
      value={query}
      onChange={handleChange}
      placeholder={placeholder}
      aria-label="Search players by name"
    />
  );
}

export default SearchBar;
