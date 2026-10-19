import "./SearchBar.css";

// Controlled input: the parent owns the query (so it can outlive this component)
// and receives every keystroke through onSearch.
function SearchBar({ value, onSearch, placeholder = "Search players…" }) {
  return (
    <input
      type="text"
      className="search-bar"
      value={value}
      onChange={(event) => onSearch(event.target.value)}
      placeholder={placeholder}
      aria-label="Search players by name"
    />
  );
}

export default SearchBar;
