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

  describe("side-by-side view", () => {
    // ACWR: LG 40/65.5 = 0.61 (Ramping up), RT 57/68 = 0.84 (Normal), C 66/65 = 1.02 (Normal).
    const C = {
      id: 30,
      name: "Creed Humphrey",
      team: "KC",
      position: "C",
      snapsLastGame: 66,
      snapsLast4Games: 260,
    };

    async function compareWith(name, watchlistedPlayers = [RT, C]) {
      const user = userEvent.setup();
      const panel = await openCompare(watchlistedPlayers);
      await user.click(
        within(panel).getByRole("button", { name: `Compare with ${name}` }),
      );
      return {
        user,
        panel,
        view: within(panel).getByRole("region", {
          name: "Side-by-side comparison",
        }),
      };
    }

    function cell(view, rowLabel, playerName) {
      const columns = within(view).getAllByRole("columnheader");
      const column = columns.findIndex((th) => th.textContent === playerName);
      const row = within(view).getByRole("rowheader", { name: rowLabel })
        .parentElement;
      return within(row).getAllByRole("cell")[column];
    }

    it("shows all four stats for both players", async () => {
      const { view } = await compareWith("Jawaan Taylor");

      for (const label of [
        "ACWR",
        "Risk tier",
        "Last game snaps",
        "4-game average",
      ]) {
        expect(
          within(view).getByRole("rowheader", { name: label }),
        ).toBeInTheDocument();
      }
      expect(cell(view, "ACWR", "Isaac Seumalo")).toHaveTextContent("0.61");
      expect(cell(view, "ACWR", "Jawaan Taylor")).toHaveTextContent("0.84");
      expect(cell(view, "Last game snaps", "Jawaan Taylor")).toHaveTextContent(
        "57",
      );
      expect(cell(view, "4-game average", "Isaac Seumalo")).toHaveTextContent(
        "65.50",
      );
    });

    it("highlights the riskier value in each row", async () => {
      const { view } = await compareWith("Jawaan Taylor");

      // The LG is further from 1.0 and in a higher tier; the RT played more snaps.
      expect(cell(view, "ACWR", "Isaac Seumalo")).toHaveClass("riskier");
      expect(cell(view, "Risk tier", "Isaac Seumalo")).toHaveClass("riskier");
      expect(cell(view, "Last game snaps", "Jawaan Taylor")).toHaveClass(
        "riskier",
      );
      expect(cell(view, "4-game average", "Jawaan Taylor")).toHaveClass(
        "riskier",
      );
      expect(cell(view, "ACWR", "Jawaan Taylor")).not.toHaveClass("riskier");
    });

    it("names the safer player in the verdict", async () => {
      const { view } = await compareWith("Jawaan Taylor");

      expect(
        within(view).getByText("Safer start: Jawaan Taylor."),
      ).toBeInTheDocument();
    });

    it("swaps to a different comparison player", async () => {
      const { user, panel } = await compareWith("Jawaan Taylor");

      await user.click(
        within(panel).getByRole("button", { name: "Compare with Creed Humphrey" }),
      );

      const view = within(panel).getByRole("region", {
        name: "Side-by-side comparison",
      });
      expect(
        within(view).getByRole("columnheader", { name: "Creed Humphrey" }),
      ).toBeInTheDocument();
      expect(
        within(view).queryByRole("columnheader", { name: "Jawaan Taylor" }),
      ).not.toBeInTheDocument();
    });

    it("clears the comparison", async () => {
      const { user, panel, view } = await compareWith("Jawaan Taylor");

      await user.click(
        within(view).getByRole("button", { name: "Clear comparison" }),
      );

      expect(
        within(panel).queryByRole("region", { name: "Side-by-side comparison" }),
      ).not.toBeInTheDocument();
      expect(within(panel).getByText("Jawaan Taylor")).toBeInTheDocument();
    });
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

describe("PlayerPage compare panel with no watchlist candidates", () => {
  const C = {
    id: 30,
    name: "Creed Humphrey",
    team: "KC",
    position: "C",
    snapsLastGame: 66,
    snapsLast4Games: 260,
  };
  const ALL_PLAYERS = [LG, RT, C, QB];

  async function openEmptyPanel() {
    const user = userEvent.setup();
    render(
      <PlayerPage
        player={LG}
        players={ALL_PLAYERS}
        watchlistedPlayers={[QB]}
        onBack={vi.fn()}
      />,
    );
    await user.click(screen.getByRole("button", { name: "Compare" }));
    const panel = screen.getByRole("complementary", {
      name: "Compare with watchlist",
    });
    return { user, panel };
  }

  function resultNames(panel) {
    return within(panel)
      .queryAllByRole("heading", { level: 2 })
      .map((h) => h.textContent)
      .filter((text) => !text.startsWith("Compare"));
  }

  it("shows a search locked to the player's position group, without the player", async () => {
    const { panel } = await openEmptyPanel();

    expect(
      within(panel).getByRole("textbox", { name: "Search players by name" }),
    ).toBeInTheDocument();
    expect(resultNames(panel)).toEqual(["Jawaan Taylor", "Creed Humphrey"]);
  });

  it("selects a search result for the side-by-side comparison", async () => {
    const { user, panel } = await openEmptyPanel();

    await user.click(within(panel).getByRole("heading", { name: "Creed Humphrey" }));

    const view = within(panel).getByRole("region", {
      name: "Side-by-side comparison",
    });
    expect(
      within(view).getByRole("columnheader", { name: "Creed Humphrey" }),
    ).toBeInTheDocument();
    expect(within(view).getByText(/^Safer start:/)).toBeInTheDocument();
  });

  it("doesn't show the search when there are watchlist candidates", async () => {
    const user = userEvent.setup();
    render(
      <PlayerPage
        player={LG}
        players={ALL_PLAYERS}
        watchlistedPlayers={[RT]}
        onBack={vi.fn()}
      />,
    );
    await user.click(screen.getByRole("button", { name: "Compare" }));

    expect(
      screen.queryByRole("textbox", { name: "Search players by name" }),
    ).not.toBeInTheDocument();
  });
});
