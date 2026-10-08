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

// Defense tests

// Small factory so the defense fixtures stay readable.
const defender = (id, name, team, position) => ({
  id,
  name,
  team,
  position,
  snapsLastGame: 50,
  snapsLast4Games: 200,
});

// 4-3 front: 4 DL, 3 LB, 3 CB, 2 S. IDs and order follow PlayerService, so the
// last CB listed (Max Melton) is the nickelback.
const ARI_DEFENSE = [
  defender(365, "Josh Sweat", "ARI", "DE"),
  defender(366, "Roy Lopez", "ARI", "DT"),
  defender(367, "Walter Nolen III", "ARI", "DT"),
  defender(368, "Dante Stills", "ARI", "DE"),
  defender(369, "Jack Gibbens", "ARI", "LB"),
  defender(370, "Mack Wilson Sr.", "ARI", "LB"),
  defender(371, "Zaven Collins", "ARI", "LB"),
  defender(372, "Garrett Williams", "ARI", "CB"),
  defender(373, "Budda Baker", "ARI", "SS"),
  defender(374, "Andrew Wingard", "ARI", "FS"),
  defender(375, "Denzel Burke", "ARI", "CB"),
  defender(376, "Max Melton", "ARI", "CB"),
];

// 3-4 front: 3 DL, 4 LB, 3 CB, 2 S.
const DEN_DEFENSE = [
  defender(473, "Zach Allen", "DEN", "DE"),
  defender(474, "D.J. Jones", "DEN", "DT"),
  defender(475, "Eyioma Uwazurike", "DEN", "DE"),
  defender(476, "Jonah Elliss", "DEN", "LB"),
  defender(477, "Alex Singleton", "DEN", "LB"),
  defender(478, "Justin Strnad", "DEN", "LB"),
  defender(479, "Nik Bonitto", "DEN", "LB"),
  defender(480, "Pat Surtain II", "DEN", "CB"),
  defender(481, "Talanoa Hufanga", "DEN", "SS"),
  defender(482, "Brandon Jones", "DEN", "FS"),
  defender(483, "Riley Moss", "DEN", "CB"),
  defender(484, "Ja'Quan McMillian", "DEN", "CB"),
];

// Reads the player names in one formation row, left to right.
const namesInRow = (container, rowClass) => {
  const row = container.querySelector(`.${rowClass}`);
  return within(row)
    .getAllByRole("heading", { level: 2 })
    .map((heading) => heading.textContent);
};

const renderDefense = (team, players) =>
  render(
    <TeamRosterPage
      team={team}
      players={players}
      unit="defense"
      onPlayerClick={vi.fn()}
    />,
  );

test("defense view renders every defender for the team and excludes other teams", () => {
  // Arrange
  const players = [...ARI_DEFENSE, ...DEN_DEFENSE];

  // Act
  renderDefense("ARI", players);

  // Assert
  expect(screen.getAllByRole("heading", { level: 2 })).toHaveLength(
    ARI_DEFENSE.length,
  );
  expect(screen.queryByText("Pat Surtain II")).not.toBeInTheDocument();
});

test("4-3 defense lays out secondary, linebackers, then defensive line in the Madden order", () => {
  // Act
  const { container } = renderDefense("ARI", ARI_DEFENSE);

  // Assert — CB1, nickelback, FS, SS, CB2
  expect(namesInRow(container, "top-row")).toEqual([
    "Garrett Williams",
    "Max Melton",
    "Andrew Wingard",
    "Budda Baker",
    "Denzel Burke",
  ]);
  expect(namesInRow(container, "middle-row")).toEqual([
    "Jack Gibbens",
    "Mack Wilson Sr.",
    "Zaven Collins",
  ]);
  // DE DT DT DE
  expect(namesInRow(container, "bottom-row")).toEqual([
    "Josh Sweat",
    "Roy Lopez",
    "Walter Nolen III",
    "Dante Stills",
  ]);
});

test("3-4 defense shows four linebackers and a three-man line", () => {
  // Act
  const { container } = renderDefense("DEN", DEN_DEFENSE);

  // Assert
  expect(namesInRow(container, "top-row")).toEqual([
    "Pat Surtain II",
    "Ja'Quan McMillian",
    "Brandon Jones",
    "Talanoa Hufanga",
    "Riley Moss",
  ]);
  expect(namesInRow(container, "middle-row")).toHaveLength(4);
  // DE DT DE
  expect(namesInRow(container, "bottom-row")).toEqual([
    "Zach Allen",
    "D.J. Jones",
    "Eyioma Uwazurike",
  ]);
});

test("the last cornerback in the data lands in the nickel slot, second from the left", () => {
  // Act
  const { container } = renderDefense("ARI", ARI_DEFENSE);

  // Assert
  expect(namesInRow(container, "top-row")[1]).toBe("Max Melton");
});

test("clicking a defender passes that player's full data to the click handler", () => {
  // Arrange
  const handlePlayerClick = vi.fn();
  render(
    <TeamRosterPage
      team="ARI"
      players={ARI_DEFENSE}
      unit="defense"
      onPlayerClick={handlePlayerClick}
    />,
  );

  // Act
  fireEvent.click(screen.getByText("Budda Baker"));

  // Assert
  expect(handlePlayerClick).toHaveBeenCalledTimes(1);
  expect(handlePlayerClick).toHaveBeenCalledWith(
    expect.objectContaining({ id: 373, name: "Budda Baker", position: "SS" }),
  );
});
