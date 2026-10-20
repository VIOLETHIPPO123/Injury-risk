import { useId, useState } from "react";
import "./SearchBar.css";

// Controlled input: the parent owns the query (so it can outlive this component)
// and receives every keystroke through onSearch. Enter with no suggestion highlighted
// calls onSubmit.
// Follows the ARIA combobox pattern: focus stays in the input while the arrow keys move
// a highlight (aria-activedescendant) through the suggestions listbox.
function SearchBar({
  value,
  onSearch,
  suggestions = [],
  onSelect,
  onSubmit,
  placeholder = "Search players…",
}) {
  const [open, setOpen] = useState(false);
  const [highlighted, setHighlighted] = useState(-1);
  const listId = useId();
  const optionId = (index) => `${listId}-option-${index}`;

  const showList = open && suggestions.length > 0;

  function close() {
    setOpen(false);
    setHighlighted(-1);
  }

  function select(player) {
    close();
    onSelect?.(player);
  }

  // Up/Down wrap around the list; Down from the input highlights the first suggestion.
  function move(step) {
    if (suggestions.length === 0) return;
    setOpen(true);
    setHighlighted((current) => {
      if (!showList || current === -1) return step > 0 ? 0 : suggestions.length - 1;
      return (current + step + suggestions.length) % suggestions.length;
    });
  }

  function handleKeyDown(event) {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      move(1);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      move(-1);
    } else if (event.key === "Enter") {
      event.preventDefault();
      // With nothing highlighted, Enter closes the list and submits the query for the results grid.
      if (showList && highlighted >= 0) {
        select(suggestions[highlighted]);
      } else {
        close();
        onSubmit?.(value);
      }
    } else if (event.key === "Escape") {
      close();
    }
  }

  return (
    <div className="search-combobox">
      <input
        type="text"
        className="search-bar"
        role="combobox"
        aria-label="Search players by name"
        aria-autocomplete="list"
        aria-expanded={showList}
        aria-controls={listId}
        aria-activedescendant={
          showList && highlighted >= 0 ? optionId(highlighted) : undefined
        }
        value={value}
        onChange={(event) => {
          onSearch(event.target.value);
          setOpen(true);
          setHighlighted(-1);
        }}
        onKeyDown={handleKeyDown}
        onBlur={close}
        placeholder={placeholder}
      />
      <ul
        id={listId}
        role="listbox"
        aria-label="Player suggestions"
        className="search-suggestions"
        hidden={!showList}
      >
        {showList &&
          suggestions.map((player, index) => (
            <li
              key={player.id}
              id={optionId(index)}
              role="option"
              aria-selected={index === highlighted}
              className={index === highlighted ? "highlighted" : undefined}
              // Keep focus in the input so its blur doesn't close the list before the click lands.
              onMouseDown={(event) => event.preventDefault()}
              onMouseEnter={() => setHighlighted(index)}
              onClick={() => select(player)}
            >
              <span className="search-suggestion-name">{player.name}</span>
              <span className="search-suggestion-meta">
                {player.team} · {player.position}
              </span>
            </li>
          ))}
      </ul>
    </div>
  );
}

export default SearchBar;
