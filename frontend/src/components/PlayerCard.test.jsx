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

test("clicking Add to Watchlist passes the player id without opening the player", () => {
  // Arrange
  const handleAdd = vi.fn();
  const handleCardClick = vi.fn();
  render(
    <PlayerCard
      {...PLAYER}
      onClick={handleCardClick}
      onAddToWatchlist={handleAdd}
    />,
  );

  // Act
  fireEvent.click(screen.getByText("Add to Watchlist"));

  // Assert
  expect(handleAdd).toHaveBeenCalledTimes(1);
  expect(handleAdd).toHaveBeenCalledWith(7);
  expect(handleCardClick).not.toHaveBeenCalled();
});

test("a player already on the watchlist gets a disabled button", () => {
  // Arrange
  render(<PlayerCard {...PLAYER} onAddToWatchlist={vi.fn()} isWatchlisted />);

  // Assert
  expect(screen.getByText("On watchlist")).toBeDisabled();
});

test("no watchlist button is shown when no add action is passed", () => {
  // Arrange
  render(<PlayerCard {...PLAYER} />);

  // Assert
  expect(screen.queryByText("Add to Watchlist")).not.toBeInTheDocument();
});
