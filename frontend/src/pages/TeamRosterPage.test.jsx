import { fireEvent, render, screen, within } from "@testing-library/react";
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

// Reads the player names in one row of a formation, left to right.
const namesInRow = (container, formationClass, rowClass) => {
  const row = container.querySelector(`.${formationClass} .${rowClass}`);
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
  expect(namesInRow(container, "defense-formation", "top-row")).toEqual([
    "Garrett Williams",
    "Max Melton",
    "Andrew Wingard",
    "Budda Baker",
    "Denzel Burke",
  ]);
  expect(namesInRow(container, "defense-formation", "middle-row")).toEqual([
    "Jack Gibbens",
    "Mack Wilson Sr.",
    "Zaven Collins",
  ]);
  // DE DT DT DE
  expect(namesInRow(container, "defense-formation", "bottom-row")).toEqual([
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
  expect(namesInRow(container, "defense-formation", "top-row")).toEqual([
    "Pat Surtain II",
    "Ja'Quan McMillian",
    "Brandon Jones",
    "Talanoa Hufanga",
    "Riley Moss",
  ]);
  expect(namesInRow(container, "defense-formation", "middle-row")).toHaveLength(
    4,
  );
  // DE DT DE
  expect(namesInRow(container, "defense-formation", "bottom-row")).toEqual([
    "Zach Allen",
    "D.J. Jones",
    "Eyioma Uwazurike",
  ]);
});

test("the last cornerback in the data lands in the nickel slot, second from the left", () => {
  // Act
  const { container } = renderDefense("ARI", ARI_DEFENSE);

  // Assert
  expect(namesInRow(container, "defense-formation", "top-row")[1]).toBe(
    "Max Melton",
  );
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

// Special teams tests

// Small factory so the special teams fixtures stay readable.
const specialist = (id, name, team, position) => ({
  id,
  name,
  team,
  position,
  snapsLastGame: 10,
  snapsLast4Games: 40,
});

// Each team has 6 specialists: PK, P, H, PR, KR, LS. IDs and order follow PlayerService.
const ARI_SPECIAL_TEAMS = [
  specialist(749, "Chad Ryland", "ARI", "PK"),
  specialist(750, "Blake Gillikin", "ARI", "P"),
  specialist(751, "Blake Gillikin", "ARI", "H"),
  specialist(752, "Devin Duvernay", "ARI", "PR"),
  specialist(753, "Devin Duvernay", "ARI", "KR"),
  specialist(754, "Casey Kreiter", "ARI", "LS"),
];

const renderSpecialTeams = (team, players) => {
  return render(
    <TeamRosterPage
      team={team}
      players={players}
      unit="special-teams"
      onPlayerClick={vi.fn()}
    />,
  );
};

test("special teams lineup renders one row in PK, P, H, PR, KR, LS order", () => {
  // Act
  const { container } = renderSpecialTeams("ARI", ARI_SPECIAL_TEAMS);

  // Assert
  expect(
    container.querySelector(".special-teams-formation"),
  ).toBeInTheDocument();
  expect(namesInRow(container, "special-teams-formation", "top-row")).toEqual([
    "Chad Ryland", // PK
    "Blake Gillikin", // P
    "Blake Gillikin", // H
    "Devin Duvernay", // PR
    "Devin Duvernay", // KR
    "Casey Kreiter", // LS
  ]);
});

test("special teams view renders six cards and excludes other teams", () => {
  // Arrange
  const players = [
    ...ARI_SPECIAL_TEAMS,
    specialist(755, "Nick Folk", "ATL", "PK"),
  ];

  // Act
  renderSpecialTeams("ARI", players);

  // Assert
  expect(screen.getAllByRole("heading", { level: 2 })).toHaveLength(6);
  expect(screen.queryByText("Nick Folk")).not.toBeInTheDocument();
});

test("a returner who also starts on offense shows only as PR and KR in the special teams view", () => {
  // Arrange: Barion Brown is NO's WR and also its PR/KR
  const players = [
    specialist(255, "Barion Brown", "NO", "WR"),
    specialist(1, "Daniel Carlson", "NO", "PK"),
    specialist(2, "Ryan Wright", "NO", "P"),
    specialist(3, "Ryan Wright", "NO", "H"),
    specialist(4, "Barion Brown", "NO", "PR"),
    specialist(5, "Barion Brown", "NO", "KR"),
    specialist(6, "Cal Adomitis", "NO", "LS"),
  ];

  // Act
  const { container } = renderSpecialTeams("NO", players);

  // Assert
  expect(
    namesInRow(container, "special-teams-formation", "top-row"),
  ).toHaveLength(6);
  expect(screen.getAllByText("Barion Brown")).toHaveLength(2); // PR and KR, not the WR row
});

// Unit toggle tests
test("toggle defaults to offense", () => {
  // Act
  const { container } = render(
    <TeamRosterPage
      team="ARI"
      players={[...ARI_STARTERS, ...ARI_DEFENSE, ...ARI_SPECIAL_TEAMS]}
      onPlayerClick={vi.fn()}
    />,
  );

  // Assert
  expect(container.querySelector(".offense-formation")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Offense" })).toHaveAttribute(
    "aria-pressed",
    "true",
  );
  expect(screen.getByRole("button", { name: "Defense" })).toHaveAttribute(
    "aria-pressed",
    "false",
  );
  expect(screen.getByRole("button", { name: "Special Teams" })).toHaveAttribute(
    "aria-pressed",
    "false",
  );
});

test("toggle switches between offense, defense and special teams lineups", () => {
  // Arrange
  const { container } = render(
    <TeamRosterPage
      team="ARI"
      players={[...ARI_STARTERS, ...ARI_DEFENSE, ...ARI_SPECIAL_TEAMS]}
      onPlayerClick={vi.fn()}
    />,
  );

  // Act + Assert: defense
  fireEvent.click(screen.getByRole("button", { name: "Defense" }));
  expect(container.querySelector(".defense-formation")).toBeInTheDocument();
  expect(container.querySelector(".offense-formation")).not.toBeInTheDocument();
  expect(screen.getAllByRole("heading", { level: 2 })).toHaveLength(
    ARI_DEFENSE.length,
  );

  // Act + Assert: special teams
  fireEvent.click(screen.getByRole("button", { name: "Special Teams" }));
  expect(
    container.querySelector(".special-teams-formation"),
  ).toBeInTheDocument();
  expect(container.querySelector(".defense-formation")).not.toBeInTheDocument();
  expect(screen.getAllByRole("heading", { level: 2 })).toHaveLength(
    ARI_SPECIAL_TEAMS.length,
  );

  // Act + Assert: back to offense
  fireEvent.click(screen.getByRole("button", { name: "Offense" }));
  expect(container.querySelector(".offense-formation")).toBeInTheDocument();
});

test("switching to a different team resets the toggle to offense", () => {
  // Arrange
  const players = [...ARI_STARTERS, ...ARI_DEFENSE, ...DEN_DEFENSE];
  const { container, rerender } = render(
    <TeamRosterPage team="ARI" players={players} onPlayerClick={vi.fn()} />,
  );
  fireEvent.click(screen.getByRole("button", { name: "Defense" }));
  expect(container.querySelector(".defense-formation")).toBeInTheDocument();

  // Act
  rerender(
    <TeamRosterPage team="DEN" players={players} onPlayerClick={vi.fn()} />,
  );

  // Assert
  expect(container.querySelector(".offense-formation")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Offense" })).toHaveAttribute(
    "aria-pressed",
    "true",
  );
});
