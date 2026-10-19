import "./FilterChips.css";

// A labelled group of toggle chips. Clicking a chip adds or removes it from `selected`;
// any number can be on at once.
function FilterChips({ label, options, selected, onChange }) {
  function toggle(option) {
    onChange(
      selected.includes(option)
        ? selected.filter((value) => value !== option)
        : [...selected, option],
    );
  }

  return (
    <fieldset className="filter-chips">
      <legend>{label}</legend>
      {options.map((option) => {
        const isSelected = selected.includes(option);
        return (
          <button
            key={option}
            type="button"
            className={`filter-chip ${isSelected ? "selected" : ""}`}
            aria-pressed={isSelected}
            onClick={() => toggle(option)}
          >
            {option}
          </button>
        );
      })}
    </fieldset>
  );
}

export default FilterChips;
