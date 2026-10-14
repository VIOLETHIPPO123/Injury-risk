import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import PlayerPage from "./PlayerPage";

const LG = {
  id: 8,
  name: "Isaac Seumalo",
  team: "ARI",
  position: "LG",
  snapsLastGame: 40,
  snapsLast4Games: 262,
};

const RT = {
  id: 22,
  name: "Jawaan Taylor",
  team: "ATL",
  position: "RT",
  snapsLastGame: 57,
  snapsLast4Games: 272,
};

const QB = {
  id: 23,
  name: "Lamar Jackson",
  team: "BAL",
  position: "QB",
  snapsLastGame: 63,
  snapsLast4Games: 261,
};

async function openCompare(watchlistedPlayers) {
  const user = userEvent.setup();
  render(
    <PlayerPage
      player={LG}
      watchlistedPlayers={watchlistedPlayers}
      onBack={vi.fn()}
    />,
  );
  await user.click(screen.getByRole("button", { name: "Compare" }));
  return screen.getByRole("complementary", { name: "Compare with watchlist" });
}

describe("PlayerPage compare panel", () => {
  it("is closed until Compare is clicked", () => {
    render(<PlayerPage player={LG} watchlistedPlayers={[RT]} onBack={vi.fn()} />);

    expect(
      screen.queryByRole("complementary", { name: "Compare with watchlist" }),
    ).not.toBeInTheDocument();
  });

  it("opens without leaving the player page", async () => {
    await openCompare([RT]);

    expect(
      screen.getByRole("heading", { level: 1, name: "Isaac Seumalo" }),
    ).toBeInTheDocument();
  });

  it("lists a watchlisted RT on an LG's panel", async () => {
    const panel = await openCompare([RT]);

    expect(within(panel).getByText("Jawaan Taylor")).toBeInTheDocument();
  });

  it("leaves out watchlisted players from other position groups", async () => {
    const panel = await openCompare([RT, QB]);

    expect(within(panel).queryByText("Lamar Jackson")).not.toBeInTheDocument();
  });

  it("treats the list as empty when the only same-group player is the current one", async () => {
    const panel = await openCompare([LG, QB]);

    expect(within(panel).queryByRole("listitem")).not.toBeInTheDocument();
    expect(
      within(panel).getByText("No other OL players on your watchlist."),
    ).toBeInTheDocument();
  });

  it("closes when Close is clicked", async () => {
    const user = userEvent.setup();
    const panel = await openCompare([RT]);

    await user.click(within(panel).getByRole("button", { name: "Close" }));

    expect(
      screen.queryByRole("complementary", { name: "Compare with watchlist" }),
    ).not.toBeInTheDocument();
  });
});
