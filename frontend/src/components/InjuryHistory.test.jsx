import { render, screen, within } from "@testing-library/react";
import { expect, test } from "vitest";
import InjuryHistory from "./InjuryHistory";

const injury = (season, week, bodyPart, type, gamesMissed, status) => ({
  season, week, bodyPart, type, gamesMissed, status,
});

// Newest first, as GET /players/{id}/injuries returns them.
const MCCAFFREY = [
  injury(2026, 6, "Calf", "Strain", 1, "Recovering"),
  injury(2025, 9, "Hamstring", "Strain", 2, "Recovered"),
  injury(2024, 1, "Achilles", "Tendinitis", 8, "Recovered"),
  injury(2023, 5, "Hamstring", "Strain", 1, "Recovered"),
];

function tableRows() {
  return within(screen.getByRole("table"))
    .getAllByRole("row")
    .slice(1)
    .map((row) => within(row).getAllByRole("cell").map((cell) => cell.textContent));
}

test("is labeled as sample data", () => {
  render(<InjuryHistory injuries={[]} />);

  const section = screen.getByRole("region", { name: "Injury history" });
  expect(within(section).getByText("Sample data")).toBeInTheDocument();
});

test("a player with no injuries shows No recorded injuries", () => {
  render(<InjuryHistory injuries={[]} />);

  expect(screen.getByText("No recorded injuries.")).toBeInTheDocument();
  expect(screen.queryByRole("table")).not.toBeInTheDocument();
  expect(screen.queryByRole("note", { name: "Repeat injuries" })).not.toBeInTheDocument();
});

test("one injury shows every field in a single row, with a summary and no warning", () => {
  render(<InjuryHistory injuries={[injury(2026, 4, "Ankle", "Sprain", 1, "Recovering")]} />);

  expect(tableRows()).toEqual([["2026", "4", "Ankle", "Sprain", "1", "Recovering"]]);
  expect(screen.getByText("1 injury in 1 season.")).toBeInTheDocument();
  expect(screen.queryByRole("note", { name: "Repeat injuries" })).not.toBeInTheDocument();
});

test("a repeated body part shows a warning above the list", () => {
  render(<InjuryHistory injuries={MCCAFFREY} />);

  const warning = screen.getByRole("note", { name: "Repeat injuries" });
  expect(warning).toHaveTextContent("2 hamstring injuries in 2 seasons.");
  // The warning sits above the list.
  expect(warning.compareDocumentPosition(screen.getByRole("table")) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
});

test("lists injuries in the order and with the fields the API sent", () => {
  render(<InjuryHistory injuries={MCCAFFREY} />);

  expect(tableRows()).toEqual([
    ["2026", "6", "Calf", "Strain", "1", "Recovering"],
    ["2025", "9", "Hamstring", "Strain", "2", "Recovered"],
    ["2024", "1", "Achilles", "Tendinitis", "8", "Recovered"],
    ["2023", "5", "Hamstring", "Strain", "1", "Recovered"],
  ]);
  expect(
    within(screen.getByRole("table")).getAllByRole("columnheader").map((h) => h.textContent),
  ).toEqual(["Season", "Week", "Body part", "Type", "Games missed", "Status"]);
});

test("shows a loading message until the injuries arrive", () => {
  render(<InjuryHistory injuries={null} />);

  expect(screen.getByText("Loading injury history…")).toBeInTheDocument();
});

test("shows the error when the injuries couldn't be loaded", () => {
  render(<InjuryHistory injuries={null} error="GET /players/7/injuries failed with status 500" />);

  expect(
    screen.getByText("Couldn't load injury history: GET /players/7/injuries failed with status 500"),
  ).toBeInTheDocument();
});
