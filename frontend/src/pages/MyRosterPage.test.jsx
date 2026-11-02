import { fireEvent, render, screen } from "@testing-library/react";
import { expect, test, vi } from "vitest";
import MyRosterPage from "./MyRosterPage";

const PLAYERS = [
  {
    id: 7,
    name: "Jacoby Brissett",
    team: "ARI",
    position: "QB",
    snapsLastGame: 68,
    snapsLast4Games: 253,
  },
];

const ENTRIES = [{ id: 1, playerId: 7, note: "Monitor snaps" }];

function renderPage(overrides = {}) {
  const props = {
    entries: ENTRIES,
    players: PLAYERS,
    onBack: vi.fn(),
    onUpdate: vi.fn().mockResolvedValue(true),
    onRemove: vi.fn(),
    ...overrides,
  };
  render(<MyRosterPage {...props} />);
  return props;
}

test("renders each rostered player with its note", () => {
  // Arrange
  renderPage();

  // Assert
  expect(screen.getByText("Jacoby Brissett")).toBeInTheDocument();
  expect(screen.getByText("Monitor snaps")).toBeInTheDocument();
});

test("shows an empty message when nothing is on the roster", () => {
  // Arrange
  renderPage({ entries: [] });

  // Assert
  expect(screen.getByText(/Your roster is empty/)).toBeInTheDocument();
});

test("editing a note and saving calls onUpdate with the entry id and new note", async () => {
  // Arrange
  const { onUpdate } = renderPage();

  // Act
  fireEvent.click(screen.getByText("Edit"));
  fireEvent.change(screen.getByLabelText("Note"), {
    target: { value: "Snap share dropping" },
  });
  fireEvent.click(screen.getByText("Save"));

  // Assert
  expect(onUpdate).toHaveBeenCalledWith(1, "Snap share dropping");
  // The form closes once the save succeeds.
  expect(await screen.findByText("Edit")).toBeInTheDocument();
});

test("clicking Remove calls onRemove with the entry id", () => {
  // Arrange
  const { onRemove } = renderPage();

  // Act
  fireEvent.click(screen.getByText("Remove"));

  // Assert
  expect(onRemove).toHaveBeenCalledTimes(1);
  expect(onRemove).toHaveBeenCalledWith(1);
});

test("shows each player's slot and how full the roster is", () => {
  // Arrange
  renderPage({ slots: new Map([[7, { id: "QB", label: "QB" }]]), limit: 16 });

  // Assert
  expect(screen.getByRole("heading", { name: "My Roster" })).toBeInTheDocument();
  expect(screen.getByText("1 / 16 players")).toBeInTheDocument();
  expect(screen.getByText("QB", { selector: ".slot" })).toBeInTheDocument();
  // The slot already says QB, so the position isn't repeated.
  expect(screen.getAllByText("QB")).toHaveLength(1);
});

test("shows the position too when the slot doesn't say it", () => {
  // Arrange
  renderPage({ slots: new Map([[7, { id: "BN", label: "Bench" }]]), limit: 16 });

  // Assert
  expect(screen.getByText("Bench", { selector: ".slot" })).toBeInTheDocument();
  expect(screen.getByText("QB", { selector: ".position" })).toBeInTheDocument();
});
