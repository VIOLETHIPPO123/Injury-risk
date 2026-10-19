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
  return user.type(screen.getByRole("textbox", { name: "Search players by name" }), text);
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

test("a 2-character query shows matching players, last-name matches first", async () => {
  const user = userEvent.setup();
  renderPage();

  await typeQuery(user, "la");

  // Lamb matches on his last name, so he ranks above Landon Dickerson, who matches on his first.
  expect(shownNames()).toEqual(["CeeDee Lamb", "Landon Dickerson"]);
  expect(screen.queryByText("Search a name or pick a position/team.")).not.toBeInTheDocument();
});

test("a query with no matches says No players found", async () => {
  const user = userEvent.setup();
  renderPage();

  await typeQuery(user, "zz");

  expect(shownNames()).toEqual([]);
  expect(screen.getByText("No players found")).toBeInTheDocument();
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

  await user.type(screen.getByRole("textbox", { name: "Search players by name" }), "brown");
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
  const searchBox = screen.getByRole("textbox", { name: "Search players by name" });

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
  renderPage({ search: { query: "dak", positions: ["QB"], teams: [] }, onSearchChange });

  expect(screen.getByRole("textbox", { name: "Search players by name" })).toHaveValue("dak");
  expect(chip("Position", "QB")).toHaveAttribute("aria-pressed", "true");
  expect(shownNames()).toEqual(["Dak Prescott"]);

  await user.click(chip("Team", "DAL"));
  expect(onSearchChange).toHaveBeenLastCalledWith({ query: "dak", positions: ["QB"], teams: ["DAL"] });

  await user.click(screen.getByRole("button", { name: "Reset" }));
  expect(onSearchChange).toHaveBeenLastCalledWith(EMPTY_SEARCH);
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

  await typeQuery(user, "cam");
  expect(shownNames()).toEqual(["Cam Jurgens"]);
});

test("in locked mode, position selections from a shared search are ignored", () => {
  renderPage({
    lockedPositions: OFFENSIVE_LINE,
    search: { query: "", positions: ["WR"], teams: [] },
    onSearchChange: () => {},
  });

  expect(shownNames()).toEqual([]);
  expect(screen.getByText("Search a name or pick a team.")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Reset" })).toBeDisabled();
});
