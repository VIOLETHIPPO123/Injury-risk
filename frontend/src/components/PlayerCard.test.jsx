import { fireEvent, render, screen } from "@testing-library/react";
import { expect, test, vi } from "vitest";
import PlayerCard from "./PlayerCard";

const PLAYER = {
  id: 7,
  name: "Jacoby Brissett",
  team: "ARI",
  position: "QB",
  snapsLastGame: 68,
  snapsLast4Games: 253,
};

test("renders the player's name, team and position", () => {
  // Arrange
  render(<PlayerCard {...PLAYER} />);

  // Assert
  expect(screen.getByText("Jacoby Brissett")).toBeInTheDocument();
  expect(screen.getByText("ARI")).toBeInTheDocument();
  expect(screen.getByText("QB")).toBeInTheDocument();
});

// What App passes in: nothing rostered, room left in a 16-player roster.
function roster(changes = {}) {
  return { ids: new Set(), isFull: false, limit: 16, onAdd: vi.fn(), ...changes };
}

test("clicking Add to roster passes the player id without opening the player", () => {
  // Arrange
  const controls = roster();
  const handleCardClick = vi.fn();
  render(<PlayerCard {...PLAYER} onClick={handleCardClick} roster={controls} />);

  // Act
  fireEvent.click(screen.getByRole("button", { name: "Add to roster" }));

  // Assert
  expect(controls.onAdd).toHaveBeenCalledTimes(1);
  expect(controls.onAdd).toHaveBeenCalledWith(7);
  expect(handleCardClick).not.toHaveBeenCalled();
});

test("a rostered player shows the On my roster badge instead of the button", () => {
  // Arrange
  render(<PlayerCard {...PLAYER} roster={roster({ ids: new Set([7]) })} />);

  // Assert
  expect(screen.getByText("On my roster")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Add to roster" })).not.toBeInTheDocument();
});

test("a player in an ineligible position has no add button", () => {
  // Arrange
  render(<PlayerCard {...PLAYER} position="LT" roster={roster()} />);

  // Assert
  expect(screen.queryByRole("button", { name: /roster/i })).not.toBeInTheDocument();
});

test("when the roster is full, the add button is disabled and names the limit", () => {
  // Arrange
  render(<PlayerCard {...PLAYER} roster={roster({ isFull: true })} />);

  // Assert
  expect(screen.getByRole("button", { name: "Roster full (16)" })).toBeDisabled();
});

test("no roster button or badge is shown without roster controls", () => {
  // Arrange
  render(<PlayerCard {...PLAYER} />);

  // Assert
  expect(screen.queryByText("Add to roster")).not.toBeInTheDocument();
  expect(screen.queryByText("On my roster")).not.toBeInTheDocument();
});
