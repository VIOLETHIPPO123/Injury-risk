import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { expect, test, vi } from "vitest";
import SearchBar from "./SearchBar";

const SUGGESTIONS = [
  { id: 1, name: "CeeDee Lamb", team: "DAL", position: "WR" },
  { id: 2, name: "Landon Dickerson", team: "PHI", position: "LG" },
  { id: 3, name: "Lane Johnson", team: "PHI", position: "RT" },
];

// SearchBar is controlled, so the tests own the query the way SearchPage does.
function Harness({ onSelect }) {
  const [query, setQuery] = useState("");
  return (
    <SearchBar
      value={query}
      onSearch={setQuery}
      suggestions={query ? SUGGESTIONS : []}
      onSelect={onSelect}
    />
  );
}

function setup() {
  const user = userEvent.setup();
  const onSelect = vi.fn();
  render(<Harness onSelect={onSelect} />);
  return { user, onSelect, input: screen.getByRole("combobox", { name: "Search players by name" }) };
}

function highlightedName() {
  const option = screen.queryAllByRole("option").find((o) => o.getAttribute("aria-selected") === "true");
  return option?.querySelector(".search-suggestion-name").textContent;
}

test("typing opens a listbox of suggestions showing name, team and position", async () => {
  const { user, input } = setup();

  expect(input).toHaveAttribute("aria-expanded", "false");
  await user.type(input, "la");

  expect(input).toHaveAttribute("aria-expanded", "true");
  const listbox = screen.getByRole("listbox", { name: "Player suggestions" });
  expect(input).toHaveAttribute("aria-controls", listbox.id);
  const options = screen.getAllByRole("option");
  expect(options).toHaveLength(3);
  expect(options[0]).toHaveTextContent("CeeDee Lamb");
  expect(options[0]).toHaveTextContent("DAL · WR");
  expect(highlightedName()).toBeUndefined();
});

test("Down and Up move the highlight, wrapping at both ends", async () => {
  const { user, input } = setup();
  await user.type(input, "la");

  await user.keyboard("{ArrowDown}");
  expect(highlightedName()).toBe("CeeDee Lamb");
  await user.keyboard("{ArrowDown}{ArrowDown}");
  expect(highlightedName()).toBe("Lane Johnson");
  await user.keyboard("{ArrowDown}");
  expect(highlightedName()).toBe("CeeDee Lamb");
  await user.keyboard("{ArrowUp}");
  expect(highlightedName()).toBe("Lane Johnson");
});

test("the highlighted option is the input's aria-activedescendant, so focus stays in the input", async () => {
  const { user, input } = setup();
  await user.type(input, "la");

  await user.keyboard("{ArrowDown}{ArrowDown}");

  expect(input).toHaveFocus();
  expect(input).toHaveAttribute("aria-activedescendant", screen.getAllByRole("option")[1].id);
});

test("Enter opens the highlighted player and closes the list", async () => {
  const { user, input, onSelect } = setup();
  await user.type(input, "la");

  await user.keyboard("{ArrowDown}{ArrowDown}{Enter}");

  expect(onSelect).toHaveBeenCalledWith(SUGGESTIONS[1]);
  expect(screen.queryAllByRole("option")).toHaveLength(0);
});

test("Enter with nothing highlighted closes the list without selecting", async () => {
  const { user, input, onSelect } = setup();
  await user.type(input, "la");

  await user.keyboard("{Enter}");

  expect(onSelect).not.toHaveBeenCalled();
  expect(input).toHaveAttribute("aria-expanded", "false");
  expect(input).toHaveValue("la");
});

test("Escape closes the list and keeps the query", async () => {
  const { user, input, onSelect } = setup();
  await user.type(input, "la");

  await user.keyboard("{ArrowDown}{Escape}");

  expect(input).toHaveAttribute("aria-expanded", "false");
  expect(input).not.toHaveAttribute("aria-activedescendant");
  expect(input).toHaveValue("la");
  expect(onSelect).not.toHaveBeenCalled();
});

test("Down reopens a closed list", async () => {
  const { user, input } = setup();
  await user.type(input, "la");

  await user.keyboard("{Escape}{ArrowDown}");

  expect(input).toHaveAttribute("aria-expanded", "true");
  expect(highlightedName()).toBe("CeeDee Lamb");
});

test("typing again after closing reopens the list with nothing highlighted", async () => {
  const { user, input } = setup();
  await user.type(input, "la");

  await user.keyboard("{ArrowDown}{Escape}");
  await user.type(input, "n");

  expect(input).toHaveAttribute("aria-expanded", "true");
  expect(highlightedName()).toBeUndefined();
});

test("clicking a suggestion selects it", async () => {
  const { user, input, onSelect } = setup();
  await user.type(input, "la");

  await user.click(screen.getByRole("option", { name: /Lane Johnson/ }));

  expect(onSelect).toHaveBeenCalledWith(SUGGESTIONS[2]);
});
