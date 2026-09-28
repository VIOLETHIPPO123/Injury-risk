import { fireEvent, render, screen } from "@testing-library/react";
import { expect, test, vi } from "vitest";
import TeamRosterPage from "./TeamRosterPage";

// A full starting offense: one of each single-count position plus three WRs,
// matching what utils/formation.js expects to lay out across the two rows.
const ARI_STARTERS = [
  {
    id: 1,
    name: "Jacoby Brissett",
    team: "ARI",
    position: "QB",
    snapsLastGame: 68,
    snapsLast4Games: 253,
  },
  {
    id: 2,
    name: "James Conner",
    team: "ARI",
    position: "RB",
    snapsLastGame: 40,
    snapsLast4Games: 150,
  },
  {
    id: 3,
    name: "Trey McBride",
    team: "ARI",
    position: "TE",
    snapsLastGame: 55,
    snapsLast4Games: 210,
  },
  {
    id: 4,
    name: "Marvin Harrison Jr.",
    team: "ARI",
    position: "WR",
    snapsLastGame: 60,
    snapsLast4Games: 230,
  },
  {
    id: 5,
    name: "Michael Wilson",
    team: "ARI",
    position: "WR",
    snapsLastGame: 58,
    snapsLast4Games: 220,
  },
  {
    id: 6,
    name: "Greg Dortch",
    team: "ARI",
    position: "WR",
    snapsLastGame: 30,
    snapsLast4Games: 110,
  },
  {
    id: 7,
    name: "Paris Johnson Jr.",
    team: "ARI",
    position: "LT",
    snapsLastGame: 70,
    snapsLast4Games: 280,
  },
  {
    id: 8,
    name: "Elijah Wilkinson",
    team: "ARI",
    position: "LG",
    snapsLastGame: 70,
    snapsLast4Games: 280,
  },
  {
    id: 9,
    name: "Hjalte Froholdt",
    team: "ARI",
    position: "C",
    snapsLastGame: 70,
    snapsLast4Games: 280,
  },
  {
    id: 10,
    name: "Will Hernandez",
    team: "ARI",
    position: "RG",
    snapsLastGame: 70,
    snapsLast4Games: 280,
  },
  {
    id: 11,
    name: "Jonah Williams",
    team: "ARI",
    position: "RT",
    snapsLastGame: 70,
    snapsLast4Games: 280,
  },
];

// A second team's players, included to confirm the page filters by team
// rather than rendering every player it's handed.
const OTHER_TEAM_PLAYER = {
  id: 99,
  name: "Patrick Mahomes",
  team: "KC",
  position: "QB",
  snapsLastGame: 65,
  snapsLast4Games: 240,
};

test("renders the expected number of players for a full lineup", () => {
  // Arrange
  const players = [...ARI_STARTERS, OTHER_TEAM_PLAYER];

  // Act
  render(
    <TeamRosterPage team="ARI" players={players} onPlayerClick={vi.fn()} />,
  );

  // Assert — one heading per rendered PlayerCard; the other team's player is excluded.
  expect(screen.getAllByRole("heading", { level: 2 })).toHaveLength(
    ARI_STARTERS.length,
  );
  expect(screen.queryByText("Patrick Mahomes")).not.toBeInTheDocument();
});

test("clicking a mini player card passes that player's full data to the click handler", () => {
  // Arrange
  const handlePlayerClick = vi.fn();
  render(
    <TeamRosterPage
      team="ARI"
      players={ARI_STARTERS}
      onPlayerClick={handlePlayerClick}
    />,
  );

  // Act
  fireEvent.click(screen.getByText("Jacoby Brissett"));

  // Assert
  expect(handlePlayerClick).toHaveBeenCalledTimes(1);
  expect(handlePlayerClick).toHaveBeenCalledWith(
    expect.objectContaining({ id: 1, name: "Jacoby Brissett", position: "QB" }),
  );
});

test("clicking a different mini player card passes that player's own data, not another player's", () => {
  // Arrange
  const handlePlayerClick = vi.fn();
  render(
    <TeamRosterPage
      team="ARI"
      players={ARI_STARTERS}
      onPlayerClick={handlePlayerClick}
    />,
  );

  // Act
  fireEvent.click(screen.getByText("Trey McBride"));

  // Assert
  expect(handlePlayerClick).toHaveBeenCalledWith(
    expect.objectContaining({ id: 3, name: "Trey McBride", position: "TE" }),
  );
});
