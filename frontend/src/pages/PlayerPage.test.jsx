import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import PlayerPage from "./PlayerPage";
import { getInjuries } from "../services/injuryService";
import { getWorkload } from "../services/workloadService";

// The player page loads injury and workload history itself; these tests answer with none
// unless they say otherwise.
vi.mock("../services/injuryService", () => ({
  getInjuries: vi.fn(() => Promise.resolve([])),
}));
vi.mock("../services/workloadService", () => ({
  getWorkload: vi.fn(() => Promise.resolve([])),
}));

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

async function openCompare(rosteredPlayers) {
  const user = userEvent.setup();
  render(
    <PlayerPage
      player={LG}
      rosteredPlayers={rosteredPlayers}
      onBack={vi.fn()}
    />,
  );
  await user.click(screen.getByRole("button", { name: "Compare" }));
  return screen.getByRole("complementary", { name: "Compare with my roster" });
}

describe("PlayerPage compare panel", () => {
  it("is closed until Compare is clicked", () => {
    render(<PlayerPage player={LG} rosteredPlayers={[RT]} onBack={vi.fn()} />);

    expect(
      screen.queryByRole("complementary", { name: "Compare with my roster" }),
    ).not.toBeInTheDocument();
  });

  it("opens without leaving the player page", async () => {
    await openCompare([RT]);

    expect(
      screen.getByRole("heading", { level: 1, name: "Isaac Seumalo" }),
    ).toBeInTheDocument();
  });

  it("lists a rostered RT on an LG's panel", async () => {
    const panel = await openCompare([RT]);

    expect(within(panel).getByText("Jawaan Taylor")).toBeInTheDocument();
  });

  it("leaves out rostered players from other position groups", async () => {
    const panel = await openCompare([RT, QB]);

    expect(within(panel).queryByText("Lamar Jackson")).not.toBeInTheDocument();
  });

  it("treats the list as empty when the only same-group player is the current one", async () => {
    const panel = await openCompare([LG, QB]);

    expect(within(panel).queryByRole("listitem")).not.toBeInTheDocument();
    expect(
      within(panel).getByText("No other OL players on your roster."),
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

    async function compareWith(name, rosteredPlayers = [RT, C]) {
      const user = userEvent.setup();
      const panel = await openCompare(rosteredPlayers);
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
      screen.queryByRole("complementary", { name: "Compare with my roster" }),
    ).not.toBeInTheDocument();
  });
});

describe("PlayerPage compare panel with no roster candidates", () => {
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
        rosteredPlayers={[QB]}
        onBack={vi.fn()}
      />,
    );
    await user.click(screen.getByRole("button", { name: "Compare" }));
    const panel = screen.getByRole("complementary", {
      name: "Compare with my roster",
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
    const { user, panel } = await openEmptyPanel();
    const searchBox = within(panel).getByRole("combobox", {
      name: "Search players by name",
    });

    // Like the search page, it starts blank until there's a query.
    expect(resultNames(panel)).toEqual([]);
    expect(within(panel).getByText("Search a name or pick a team.")).toBeInTheDocument();

    // "ja" also starts Lamar Jackson, but he's a QB, not OL.
    await user.type(searchBox, "ja{Enter}");
    expect(resultNames(panel)).toEqual(["Jawaan Taylor"]);

    // "is" starts Isaac Seumalo, the player being compared.
    await user.clear(searchBox);
    await user.type(searchBox, "is{Enter}");
    expect(resultNames(panel)).toEqual([]);
  });

  it("selects a search result for the side-by-side comparison", async () => {
    const { user, panel } = await openEmptyPanel();

    await user.type(
      within(panel).getByRole("combobox", { name: "Search players by name" }),
      "creed{Enter}",
    );
    await user.click(within(panel).getByRole("heading", { name: "Creed Humphrey" }));

    const view = within(panel).getByRole("region", {
      name: "Side-by-side comparison",
    });
    expect(
      within(view).getByRole("columnheader", { name: "Creed Humphrey" }),
    ).toBeInTheDocument();
    expect(within(view).getByText(/^Safer start:/)).toBeInTheDocument();
  });

  it("doesn't show the search when there are roster candidates", async () => {
    const user = userEvent.setup();
    render(
      <PlayerPage
        player={LG}
        players={ALL_PLAYERS}
        rosteredPlayers={[RT]}
        onBack={vi.fn()}
      />,
    );
    await user.click(screen.getByRole("button", { name: "Compare" }));

    expect(
      screen.queryByRole("combobox", { name: "Search players by name" }),
    ).not.toBeInTheDocument();
  });
});

describe("PlayerPage compare panel with roster slots", () => {
  const wr = (id, name, slot) => ({
    id, name, team: "DAL", position: "WR", snapsLastGame: 60, snapsLast4Games: 240, slot,
  });
  const LAMB = wr(40, "CeeDee Lamb", { id: "WR1", label: "WR" });
  const PICKENS = wr(41, "George Pickens", { id: "FLEX", label: "FLEX" });
  const TOLBERT = wr(42, "Jalen Tolbert", { id: "BN", label: "Bench" });
  const COOKS = wr(43, "Brandin Cooks", { id: "WR2", label: "WR" });

  it("lists the rostered players in the group with their slots, never the current player", async () => {
    const user = userEvent.setup();
    render(
      <PlayerPage player={LAMB} rosteredPlayers={[LAMB, PICKENS, TOLBERT, COOKS, QB]} onBack={vi.fn()} />,
    );
    await user.click(screen.getByRole("button", { name: "Compare" }));
    const panel = screen.getByRole("complementary", { name: "Compare with my roster" });

    const items = within(panel).getAllByRole("listitem");
    expect(items.map((item) => item.querySelector(".compare-panel-name").textContent)).toEqual([
      "George Pickens",
      "Jalen Tolbert",
      "Brandin Cooks",
    ]);
    expect(within(items[0]).getByText("FLEX · DAL · WR")).toBeInTheDocument();
    expect(within(items[1]).getByText("Bench · DAL · WR")).toBeInTheDocument();
    // A WR in a WR slot doesn't repeat the position.
    expect(within(items[2]).getByText("WR · DAL")).toBeInTheDocument();
  });
});

describe("PlayerPage injury history", () => {
  it("loads the shown player's injuries and lists them", async () => {
    getInjuries.mockResolvedValueOnce([
      { season: 2025, week: 9, bodyPart: "Hamstring", type: "Strain", gamesMissed: 2, status: "Recovered" },
      { season: 2023, week: 5, bodyPart: "Hamstring", type: "Strain", gamesMissed: 1, status: "Recovered" },
    ]);

    render(<PlayerPage player={QB} onBack={vi.fn()} />);

    expect(getInjuries).toHaveBeenCalledWith(QB.id);
    expect(await screen.findByText("⚠ 2 hamstring injuries in 2 seasons.")).toBeInTheDocument();
  });

  it("shows No recorded injuries for a player without any", async () => {
    render(<PlayerPage player={QB} onBack={vi.fn()} />);

    expect(await screen.findByText("No recorded injuries.")).toBeInTheDocument();
  });
});

describe("PlayerPage ACWR trend", () => {
  it("loads the shown player's workload and charts it", async () => {
    // Weeks 1 to 5, steady 50 snaps: one ACWR value per week from week 4.
    getWorkload.mockResolvedValueOnce(
      [1, 2, 3, 4, 5].map((week) => ({ playerId: QB.id, week, snaps: 50 })),
    );

    render(<PlayerPage player={QB} onBack={vi.fn()} />);

    expect(getWorkload).toHaveBeenCalledWith(QB.id);
    expect(await screen.findByRole("img", { name: "Week 5: 50 snaps, ACWR 1.00" })).toBeInTheDocument();
  });

  it("shows No trend data yet for a player with no history", async () => {
    render(<PlayerPage player={QB} onBack={vi.fn()} />);

    expect(await screen.findByText("No trend data yet.")).toBeInTheDocument();
  });
});
