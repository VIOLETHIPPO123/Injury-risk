import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import MyRosterPage from "./MyRosterPage";
import { assignSlots } from "../utils/rosterFormats";
import { lineupFormat, LINEUP_FORMATS } from "../utils/rosterFormats.fixture";

// ACWR = last game ÷ (last four ÷ 4).
const player = (id, name, position, snapsLastGame = 60, snapsLast4Games = 240) => ({
  id, name, team: "DAL", position, snapsLastGame, snapsLast4Games,
});
const PRESCOTT = player(7, "Dak Prescott", "QB"); // 1.0, Normal
const WILLIAMS = player(8, "Javonte Williams", "RB", 80, 200); // 1.6, Spiking — high risk
const LUEPKE = player(9, "Hunter Luepke", "FB", 50, 200); // 1.0, Normal
const LAMB = player(10, "CeeDee Lamb", "WR");

const entry = (id, playerId, note = "") => ({ id, playerId, note });

// Renders the page the way App does: slots come from assignSlots for the format.
function renderPage({ formatId = "espn-standard", roster = [PRESCOTT], assignments = {}, notes = {}, ...overrides } = {}) {
  const entries = roster.map((p, i) => entry(i + 1, p.id, notes[p.id] ?? ""));
  const props = {
    entries,
    players: roster,
    formats: LINEUP_FORMATS,
    formatId,
    slots: assignSlots(lineupFormat(formatId), roster, assignments),
    limit: 16,
    onFormatChange: vi.fn(),
    onMove: vi.fn(),
    onBack: vi.fn(),
    onUpdate: vi.fn().mockResolvedValue(true),
    onRemove: vi.fn(),
    ...overrides,
  };
  render(<MyRosterPage {...props} />);
  return props;
}

function slotLabels(listName) {
  const list = screen.getByRole("list", { name: listName });
  return within(list)
    .queryAllByRole("listitem")
    .map((li) => li.querySelector(".slot").textContent);
}

function openCount(listName) {
  return within(screen.getByRole("list", { name: listName })).queryAllByText("Open").length;
}

// ---------- Notes, removing, and the empty roster (kept from the old page) ----------

test("renders each rostered player with its note", () => {
  renderPage({ notes: { 7: "Monitor snaps" } });

  expect(screen.getByText("Dak Prescott")).toBeInTheDocument();
  expect(screen.getByText("Monitor snaps")).toBeInTheDocument();
});

test("shows an empty message when nothing is on the roster, with every slot open", () => {
  renderPage({ roster: [] });

  expect(screen.getByText(/Your roster is empty/)).toBeInTheDocument();
  expect(openCount("Starters")).toBe(9);
});

test("editing a note and saving calls onUpdate with the entry id and new note", async () => {
  const { onUpdate } = renderPage({ notes: { 7: "Monitor snaps" } });

  fireEvent.click(screen.getByText("Edit"));
  fireEvent.change(screen.getByLabelText("Note"), { target: { value: "Snap share dropping" } });
  fireEvent.click(screen.getByText("Save"));

  expect(onUpdate).toHaveBeenCalledWith(1, "Snap share dropping");
  expect(await screen.findByText("Edit")).toBeInTheDocument();
});

test("clicking Remove calls onRemove with the entry id", () => {
  const { onRemove } = renderPage();

  fireEvent.click(screen.getByText("Remove"));

  expect(onRemove).toHaveBeenCalledWith(1);
});

test("shows how full the roster is, and each player's risk tier", () => {
  renderPage();

  expect(screen.getByText("1 / 16 players")).toBeInTheDocument();
  expect(screen.getByText("ACWR 1.00 · Normal workload")).toBeInTheDocument();
});

// ---------- Layout per format ----------

describe("the lineup matches the selected format", () => {
  test("ESPN Standard: 9 starting slots, 7 bench spots and 1 IR, empty ones shown as open", () => {
    renderPage({ formatId: "espn-standard", roster: [PRESCOTT] });

    expect(slotLabels("Starters")).toEqual(["QB", "RB", "RB", "WR", "WR", "TE", "K", "DEF", "FLEX"]);
    expect(openCount("Starters")).toBe(8);
    expect(slotLabels("Bench")).toHaveLength(7);
    expect(openCount("Bench")).toBe(7);
    expect(slotLabels("IR")).toEqual(["IR"]);
    expect(openCount("IR")).toBe(1);
  });

  test("Yahoo Standard: 9 starting slots, 6 bench spots and 2 IR", () => {
    renderPage({ formatId: "yahoo-standard", roster: [] });

    expect(slotLabels("Starters")).toEqual(["QB", "RB", "RB", "WR", "WR", "TE", "K", "DEF", "FLEX"]);
    expect(slotLabels("Bench")).toHaveLength(6);
    expect(slotLabels("IR")).toHaveLength(2);
  });

  test("Sleeper Default: two FLEX slots, 5 bench spots and no IR", () => {
    renderPage({ formatId: "sleeper-default", roster: [] });

    expect(slotLabels("Starters")).toEqual(["QB", "RB", "RB", "WR", "WR", "TE", "K", "DEF", "FLEX", "FLEX"]);
    expect(slotLabels("Bench")).toHaveLength(5);
    expect(slotLabels("IR")).toHaveLength(0);
  });

  test("a benched player fills a bench spot, and the position shows when the slot doesn't say it", () => {
    renderPage({ assignments: { 7: "BN" } });

    expect(openCount("Starters")).toBe(9);
    expect(openCount("Bench")).toBe(6);
    const bench = screen.getByRole("list", { name: "Bench" });
    expect(within(bench).getByText("Dak Prescott")).toBeInTheDocument();
    expect(within(bench).getByText("QB", { selector: ".position" })).toBeInTheDocument();
  });
});

// ---------- Moving players ----------

describe("moving players", () => {
  test("slots a player's position isn't allowed in are disabled", () => {
    renderPage();

    const options = within(screen.getByRole("combobox", { name: "Move Dak Prescott" })).getAllByRole("option");
    // The allowed slots come first, then every disabled one.
    expect(options.map((o) => o.disabled)).toEqual([false, false, false, true, true, true, true, true, true, true, true]);
    expect(options.slice(0, 3).map((o) => o.textContent)).toEqual(["QB — Dak Prescott", "Bench", "IR"]);
    expect(options[3].textContent).toBe("RB — open");
    for (const rbSlot of screen.getAllByRole("option", { name: "RB — open" })) {
      expect(rbSlot).toBeDisabled();
    }
    expect(screen.getByRole("option", { name: "FLEX — open" })).toBeDisabled();
  });

  test("a fullback can move into an RB slot or FLEX", () => {
    renderPage({ roster: [LUEPKE], assignments: { 9: "BN" } });

    const select = screen.getByRole("combobox", { name: "Move Hunter Luepke" });
    expect(within(select).getAllByRole("option", { name: "RB — open" })[0]).toBeEnabled();
    expect(within(select).getByRole("option", { name: "FLEX — open" })).toBeEnabled();
    expect(within(select).getByRole("option", { name: "QB — open" })).toBeDisabled();
  });

  test("IR is disabled once it's full", () => {
    renderPage({ roster: [PRESCOTT, LAMB], assignments: { 10: "IR" } });

    const select = screen.getByRole("combobox", { name: "Move Dak Prescott" });
    expect(within(select).getByRole("option", { name: "IR" })).toBeDisabled();
  });

  test("choosing an allowed slot calls onMove with the player and slot", () => {
    const { onMove } = renderPage();

    fireEvent.change(screen.getByRole("combobox", { name: "Move Dak Prescott" }), { target: { value: "BN" } });

    expect(onMove).toHaveBeenCalledWith(7, "BN");
  });
});

// ---------- Changing the format ----------

describe("changing the format", () => {
  test("lists the three formats from the backend", () => {
    renderPage();

    const options = within(screen.getByLabelText("Format")).getAllByRole("option");
    expect(options.map((o) => o.textContent)).toEqual(["ESPN Standard", "Yahoo Standard", "Sleeper Default"]);
  });

  test("shows a loading message until the formats arrive", () => {
    renderPage({ formats: [] });

    expect(screen.getByText("Loading roster formats…")).toBeInTheDocument();
    expect(screen.queryByLabelText("Format")).not.toBeInTheDocument();
  });

  test("asks for confirmation that every player will move to the bench", () => {
    const { onFormatChange } = renderPage();

    fireEvent.change(screen.getByLabelText("Format"), { target: { value: "yahoo-standard" } });

    const dialog = screen.getByRole("alertdialog", { name: "Change format" });
    expect(dialog).toHaveTextContent("Switching to Yahoo Standard moves every player to the bench.");
    expect(onFormatChange).not.toHaveBeenCalled();

    fireEvent.click(within(dialog).getByRole("button", { name: "Switch format" }));
    expect(onFormatChange).toHaveBeenCalledWith("yahoo-standard");
    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
  });

  test("Cancel keeps the current format", () => {
    const { onFormatChange } = renderPage();

    fireEvent.change(screen.getByLabelText("Format"), { target: { value: "sleeper-default" } });
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(onFormatChange).not.toHaveBeenCalled();
    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
    expect(screen.getByLabelText("Format")).toHaveValue("espn-standard");
  });
});

// ---------- Bench suggestions ----------

describe("bench suggestions", () => {
  test("suggests a lower-risk bench player who is allowed in a high-risk starter's slot", () => {
    // Williams starts at RB and is spiking; Luepke (FB) on the bench is normal and can play RB.
    renderPage({ roster: [WILLIAMS, LUEPKE], assignments: { 8: "RB1", 9: "BN" } });

    expect(screen.getByText("Consider starting Hunter Luepke over Javonte Williams.")).toBeInTheDocument();
  });

  test("makes no suggestion when the only bench player isn't allowed in that slot", () => {
    // Lamb (WR) can't play the RB slot Williams is in.
    renderPage({ roster: [WILLIAMS, LAMB], assignments: { 8: "RB1", 10: "BN" } });

    expect(screen.queryByText(/Consider starting/)).not.toBeInTheDocument();
  });

  test("makes no suggestion when the bench player wouldn't lower the risk", () => {
    const spiking = player(11, "Rico Dowdle", "RB", 90, 200); // 1.8, also high, and further from 1.0
    renderPage({ roster: [WILLIAMS, spiking], assignments: { 8: "RB1", 11: "BN" } });

    expect(screen.queryByText(/Consider starting/)).not.toBeInTheDocument();
  });

  test("makes no suggestion when no starter is high risk", () => {
    renderPage({ roster: [LUEPKE, WILLIAMS], assignments: { 9: "RB1", 8: "BN" } });

    expect(screen.queryByText(/Consider starting/)).not.toBeInTheDocument();
  });
});
