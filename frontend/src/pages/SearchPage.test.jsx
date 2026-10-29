import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { expect, test, vi } from "vitest";
import SearchPage from "./SearchPage";
import { EMPTY_SEARCH } from "../utils/filterPlayers";

const player = (id, name, team, position) => ({
  id, name, team, position, snapsLastGame: 50, snapsLast4Games: 200,
});

const PLAYERS = [
  player(1, "CeeDee Lamb", "DAL", "WR"),
  player(2, "Dak Prescott", "DAL", "QB"),
  player(3, "Tyler Guyton", "DAL", "LT"),
  player(4, "A.J. Brown", "PHI", "WR"),
  player(5, "Saquon Barkley", "PHI", "RB"),
  player(6, "Cam Jurgens", "PHI", "C"),
  player(7, "Landon Dickerson", "PHI", "LG"),
];

const OFFENSIVE_LINE = ["LT", "LG", "C", "RG", "RT"];

function renderPage(props = {}) {
  render(<SearchPage players={PLAYERS} onBack={() => {}} {...props} />);
}

function shownNames() {
  return screen.queryAllByRole("heading", { level: 2 }).map((h) => h.textContent);
}

function chip(group, label) {
  return within(screen.getByRole("group", { name: group })).getByRole("button", { name: label });
}

function typeQuery(user, text) {
  return user.type(screen.getByRole("combobox", { name: "Search players by name" }), text);
}

// Types the query and presses Enter, which is what fills the results grid.
async function searchFor(user, text) {
  await typeQuery(user, text);
  await user.keyboard("{Enter}");
}

// ---------- Blank default and query length ----------

test("the page opens with no player cards and a prompt", () => {
  renderPage();

  expect(shownNames()).toEqual([]);
  expect(screen.getByText("Search a name or pick a position/team.")).toBeInTheDocument();
  expect(screen.queryByText("No players found")).not.toBeInTheDocument();
});

test("a 1-character query shows no results and keeps the prompt", async () => {
  const user = userEvent.setup();
  renderPage();

  await typeQuery(user, "c");

  expect(shownNames()).toEqual([]);
  expect(screen.getByText("Search a name or pick a position/team.")).toBeInTheDocument();
});

test("a 2-character query shows matching players after Enter, last-name matches first", async () => {
  const user = userEvent.setup();
  renderPage();

  await searchFor(user, "la");

  // Lamb matches on his last name, so he ranks above Landon Dickerson, who matches on his first.
  expect(shownNames()).toEqual(["CeeDee Lamb", "Landon Dickerson"]);
  expect(screen.queryByText("Search a name or pick a position/team.")).not.toBeInTheDocument();
});

test("a query with no matches says No players found", async () => {
  const user = userEvent.setup();
  renderPage();

  await searchFor(user, "zz");

  expect(shownNames()).toEqual([]);
  expect(screen.getByText("No players found")).toBeInTheDocument();
});

test("typing shows suggestions but no player cards until Enter is pressed", async () => {
  const user = userEvent.setup();
  renderPage();

  await typeQuery(user, "CeeDee Lamb");

  expect(screen.getByRole("option", { name: /CeeDee Lamb/ })).toBeInTheDocument();
  expect(shownNames()).toEqual([]);
  expect(screen.getByText("Search a name or pick a position/team.")).toBeInTheDocument();

  await user.keyboard("{Enter}");
  expect(shownNames()).toEqual(["CeeDee Lamb"]);
});

test("editing a submitted query keeps the previous results until Enter is pressed again", async () => {
  const user = userEvent.setup();
  renderPage();

  await searchFor(user, "lamb");
  await user.clear(screen.getByRole("combobox", { name: "Search players by name" }));
  await typeQuery(user, "brown");
  expect(shownNames()).toEqual(["CeeDee Lamb"]);

  await user.keyboard("{Enter}");
  expect(shownNames()).toEqual(["A.J. Brown"]);
});

// ---------- Suggestions and opening players ----------

test("suggestions show at most 6 players, in the same order as the results", async () => {
  const user = userEvent.setup();
  const many = Array.from({ length: 8 }, (_, i) => player(100 + i, `Sam Player${i}`, "DAL", "WR"));
  renderPage({ players: many });

  await typeQuery(user, "sam");
  expect(screen.getAllByRole("option")).toHaveLength(6);

  await user.keyboard("{Enter}");
  expect(shownNames()).toHaveLength(8);
});

test("filters alone don't open the suggestions", async () => {
  const user = userEvent.setup();
  renderPage();

  await user.click(chip("Team", "PHI"));

  expect(screen.queryAllByRole("option")).toHaveLength(0);
});

test("choosing a suggestion with the keyboard opens that player", async () => {
  const user = userEvent.setup();
  const onSelectPlayer = vi.fn();
  renderPage({ onSelectPlayer });

  await typeQuery(user, "la");
  await user.keyboard("{ArrowDown}{ArrowDown}{Enter}");

  expect(onSelectPlayer).toHaveBeenCalledWith(PLAYERS[6]);
});

test("Enter with nothing highlighted closes the suggestions and leaves the matches in the grid", async () => {
  const user = userEvent.setup();
  const onSelectPlayer = vi.fn();
  renderPage({ onSelectPlayer });

  await typeQuery(user, "la");
  await user.keyboard("{Enter}");

  expect(screen.queryAllByRole("option")).toHaveLength(0);
  expect(shownNames()).toEqual(["CeeDee Lamb", "Landon Dickerson"]);
  expect(onSelectPlayer).not.toHaveBeenCalled();
});

test("clicking a result card opens that player", async () => {
  const user = userEvent.setup();
  const onSelectPlayer = vi.fn();
  renderPage({ onSelectPlayer });

  await user.click(chip("Team", "PHI"));
  await user.click(screen.getByRole("heading", { name: "Saquon Barkley" }));

  expect(onSelectPlayer).toHaveBeenCalledWith(PLAYERS[4]);
});

// ---------- Filter combinations ----------

test("selecting only filters, with no query, shows results", async () => {
  const user = userEvent.setup();
  renderPage();

  await user.click(chip("Team", "PHI"));

  expect(shownNames()).toEqual(["A.J. Brown", "Saquon Barkley", "Cam Jurgens", "Landon Dickerson"]);
});

test("selecting WR and DAL shows only Dallas wide receivers", async () => {
  const user = userEvent.setup();
  renderPage();

  await user.click(chip("Position", "WR"));
  await user.click(chip("Team", "DAL"));

  expect(shownNames()).toEqual(["CeeDee Lamb"]);
});

test("selecting QB and RB shows both positions", async () => {
  const user = userEvent.setup();
  renderPage();

  await user.click(chip("Position", "QB"));
  await user.click(chip("Position", "RB"));

  expect(shownNames()).toEqual(["Dak Prescott", "Saquon Barkley"]);
});

test("chips combine with the name query", async () => {
  const user = userEvent.setup();
  renderPage();

  await searchFor(user, "brown");
  await user.click(chip("Position", "WR"));

  expect(shownNames()).toEqual(["A.J. Brown"]);
});

test("a combination with no matches says so", async () => {
  const user = userEvent.setup();
  renderPage();

  await user.click(chip("Position", "RB"));
  await user.click(chip("Team", "DAL"));

  expect(shownNames()).toEqual([]);
  expect(screen.getByText("No players found")).toBeInTheDocument();
});

// ---------- Clearing and Reset ----------

test("clicking a selected chip clears just that chip and updates the results", async () => {
  const user = userEvent.setup();
  renderPage();

  await user.click(chip("Position", "WR"));
  await user.click(chip("Team", "DAL"));
  await user.click(chip("Team", "DAL"));

  expect(chip("Team", "DAL")).toHaveAttribute("aria-pressed", "false");
  expect(chip("Position", "WR")).toHaveAttribute("aria-pressed", "true");
  expect(shownNames()).toEqual(["CeeDee Lamb", "A.J. Brown"]);
});

test("Reset clears the query and every chip and returns to the prompt", async () => {
  const user = userEvent.setup();
  renderPage();
  const searchBox = screen.getByRole("combobox", { name: "Search players by name" });

  await user.type(searchBox, "lamb");
  await user.click(chip("Position", "WR"));
  await user.click(chip("Team", "DAL"));
  await user.click(screen.getByRole("button", { name: "Reset" }));

  expect(searchBox).toHaveValue("");
  expect(chip("Position", "WR")).toHaveAttribute("aria-pressed", "false");
  expect(chip("Team", "DAL")).toHaveAttribute("aria-pressed", "false");
  expect(shownNames()).toEqual([]);
  expect(screen.getByText("Search a name or pick a position/team.")).toBeInTheDocument();
});

test("Reset is disabled until something is selected", async () => {
  const user = userEvent.setup();
  renderPage();

  expect(screen.getByRole("button", { name: "Reset" })).toBeDisabled();
  await user.click(chip("Team", "DAL"));
  expect(screen.getByRole("button", { name: "Reset" })).toBeEnabled();
});

// ---------- Parent-owned state (what App uses for persistence) ----------

test("when a parent owns the search, the page shows it and reports every change", async () => {
  const user = userEvent.setup();
  const onSearchChange = vi.fn();
  renderPage({ search: { input: "dak", query: "dak", positions: ["QB"], teams: [] }, onSearchChange });

  expect(screen.getByRole("combobox", { name: "Search players by name" })).toHaveValue("dak");
  expect(chip("Position", "QB")).toHaveAttribute("aria-pressed", "true");
  expect(shownNames()).toEqual(["Dak Prescott"]);

  await user.click(chip("Team", "DAL"));
  expect(onSearchChange).toHaveBeenLastCalledWith({
    input: "dak",
    query: "dak",
    positions: ["QB"],
    teams: ["DAL"],
    sortByRisk: false,
    sortDirection: "desc",
  });

  await user.click(screen.getByRole("button", { name: "Reset" }));
  expect(onSearchChange).toHaveBeenLastCalledWith(EMPTY_SEARCH);
});

// ---------- Sort by risk ----------

// ACWR = last game ÷ (last four ÷ 4).
const RISK_PLAYERS = [
  { id: 11, name: "Normal Nate", team: "DAL", position: "WR", snapsLastGame: 50, snapsLast4Games: 200 }, // 1.0, low
  { id: 12, name: "Nodata Ned", team: "DAL", position: "QB", snapsLastGame: 0, snapsLast4Games: 0 }, // no data
  { id: 13, name: "Spiking Sam", team: "PHI", position: "RB", snapsLastGame: 80, snapsLast4Games: 200 }, // 1.6, high
  { id: 14, name: "Elevated Eli", team: "PHI", position: "WR", snapsLastGame: 70, snapsLast4Games: 200 }, // 1.4, medium
  { id: 15, name: "Spiking Steve", team: "DAL", position: "TE", snapsLastGame: 90, snapsLast4Games: 200 }, // 1.8, high
];

function sortToggle() {
  return screen.getByRole("button", { name: "Sort by risk" });
}

test("with no query or filters, turning the sort on lists every player, highest risk first", async () => {
  const user = userEvent.setup();
  render(<SearchPage players={RISK_PLAYERS} onBack={() => {}} />);

  expect(shownNames()).toEqual([]);
  await user.click(sortToggle());

  expect(sortToggle()).toHaveAttribute("aria-pressed", "true");
  expect(screen.queryByText("Search a name or pick a position/team.")).not.toBeInTheDocument();
  expect(shownNames()).toEqual([
    "Spiking Steve",
    "Spiking Sam",
    "Elevated Eli",
    "Normal Nate",
    "Nodata Ned",
  ]);
});

test("the order picker reverses the order, and players with no data stay last as \"No data\"", async () => {
  const user = userEvent.setup();
  render(<SearchPage players={RISK_PLAYERS} onBack={() => {}} />);

  expect(screen.queryByRole("group", { name: "Sort order" })).not.toBeInTheDocument();
  await user.click(sortToggle());
  expect(screen.getByRole("button", { name: "Highest risk first" })).toHaveAttribute("aria-pressed", "true");
  await user.click(screen.getByRole("button", { name: "Lowest risk first" }));

  expect(screen.getByRole("button", { name: "Lowest risk first" })).toHaveAttribute("aria-pressed", "true");
  expect(screen.getByRole("button", { name: "Highest risk first" })).toHaveAttribute("aria-pressed", "false");
  expect(shownNames()).toEqual([
    "Normal Nate",
    "Elevated Eli",
    "Spiking Sam",
    "Spiking Steve",
    "Nodata Ned",
  ]);
  const noDataCard = screen.getByRole("heading", { name: "Nodata Ned" }).closest("article");
  expect(within(noDataCard).getByText("No data")).toBeInTheDocument();
});

test("the sort also orders filtered results", async () => {
  const user = userEvent.setup();
  render(<SearchPage players={RISK_PLAYERS} onBack={() => {}} />);

  await user.click(chip("Team", "DAL"));
  expect(shownNames()).toEqual(["Normal Nate", "Nodata Ned", "Spiking Steve"]);

  await user.click(sortToggle());
  expect(shownNames()).toEqual(["Spiking Steve", "Normal Nate", "Nodata Ned"]);
});

test("Reset turns the sort off and puts the direction back to highest first", async () => {
  const user = userEvent.setup();
  render(<SearchPage players={RISK_PLAYERS} onBack={() => {}} />);

  await user.click(sortToggle());
  await user.click(screen.getByRole("button", { name: "Lowest risk first" }));
  await user.click(screen.getByRole("button", { name: "Reset" }));

  expect(sortToggle()).toHaveAttribute("aria-pressed", "false");
  expect(screen.queryByRole("group", { name: "Sort order" })).not.toBeInTheDocument();
  expect(shownNames()).toEqual([]);
  expect(screen.getByText("Search a name or pick a position/team.")).toBeInTheDocument();

  await user.click(sortToggle());
  expect(screen.getByRole("button", { name: "Highest risk first" })).toHaveAttribute("aria-pressed", "true");
});

// ---------- Locked positions ----------

test("with lockedPositions, the position chips are hidden and the page opens on a team prompt", () => {
  renderPage({ lockedPositions: OFFENSIVE_LINE });

  expect(screen.queryByRole("group", { name: "Position" })).not.toBeInTheDocument();
  expect(shownNames()).toEqual([]);
  expect(screen.getByText("Search a name or pick a team.")).toBeInTheDocument();
});

test("in locked mode, team chips and the query only return the locked positions", async () => {
  const user = userEvent.setup();
  renderPage({ lockedPositions: OFFENSIVE_LINE });

  await user.click(chip("Team", "PHI"));
  expect(shownNames()).toEqual(["Cam Jurgens", "Landon Dickerson"]);

  await searchFor(user, "cam");
  expect(shownNames()).toEqual(["Cam Jurgens"]);
});

test("in locked mode, position selections from a shared search are ignored", () => {
  renderPage({
    lockedPositions: OFFENSIVE_LINE,
    search: { ...EMPTY_SEARCH, positions: ["WR"] },
    onSearchChange: () => {},
  });

  expect(shownNames()).toEqual([]);
  expect(screen.getByText("Search a name or pick a team.")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Reset" })).toBeDisabled();
});

// ---------- Choosing a result ----------

test("with onSelectPlayer, clicking a result reports that player", async () => {
  const user = userEvent.setup();
  const onSelectPlayer = vi.fn();
  renderPage({ onSelectPlayer });

  await user.type(screen.getByRole("combobox", { name: "Search players by name" }), "cam{Enter}");
  await user.click(screen.getByRole("heading", { name: "Cam Jurgens" }));

  expect(onSelectPlayer).toHaveBeenCalledWith(PLAYERS[5]);
});

test("without onBack, there is no back link", () => {
  render(<SearchPage players={PLAYERS} />);

  expect(screen.queryByText("← Back to home")).not.toBeInTheDocument();
});
