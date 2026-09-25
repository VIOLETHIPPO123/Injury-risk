import { fireEvent, render, screen } from "@testing-library/react";
import { expect, test, vi } from "vitest";
import WatchlistPage from "./WatchlistPage";

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
  render(<WatchlistPage {...props} />);
  return props;
}

test("renders each watchlisted player with its note", () => {
  // Arrange
  renderPage();

  // Assert
  expect(screen.getByText("Jacoby Brissett")).toBeInTheDocument();
  expect(screen.getByText("Monitor snaps")).toBeInTheDocument();
});

test("shows an empty message when nothing is on the watchlist", () => {
  // Arrange
  renderPage({ entries: [] });

  // Assert
  expect(screen.getByText(/Your watchlist is empty/)).toBeInTheDocument();
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
