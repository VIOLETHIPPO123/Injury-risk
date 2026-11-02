import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import AcwrTrendChart from "./AcwrTrendChart";

// Games as GET /players/{id}/workload sends them, from snaps per week starting at week 1.
// null is a bye week, which has no entry.
const games = (snapsPerWeek) =>
  snapsPerWeek
    .map((snaps, i) => ({ playerId: 1, week: i + 1, snaps }))
    .filter((game) => game.snaps !== null);

// Bye in week 5. ACWR from week 4: 1.00, (bye), 1.00, 1.14, 1.22 (rising).
const SEASON = games([50, 50, 50, 50, null, 50, 60, 70]);

const points = () => screen.getAllByRole("img", { name: /^Week \d+:/ });

// The tooltip's lines, or null when it's hidden.
const tooltip = (container) => {
  const box = container.querySelector(".acwr-trend-tooltip");
  return box && [...box.children].map((line) => line.textContent);
};

describe("AcwrTrendChart", () => {
  it("shows a point for each week with an ACWR, skipping the first three games and the bye", () => {
    render(<AcwrTrendChart games={SEASON} />);

    expect(points().map((p) => p.getAttribute("aria-label"))).toEqual([
      "Week 4: 50 snaps, ACWR 1.00",
      "Week 6: 50 snaps, ACWR 1.00",
      "Week 7: 60 snaps, ACWR 1.14",
      "Week 8: 70 snaps, ACWR 1.22",
    ]);
  });

  it("leaves a gap at the bye: the line breaks there and the axis says Bye", () => {
    const { container } = render(<AcwrTrendChart games={SEASON} />);

    // Week 4 stands alone; weeks 6 to 8 are one line.
    const lines = container.querySelectorAll(".acwr-trend-line");
    expect(lines).toHaveLength(1);
    expect(lines[0].getAttribute("points").split(" ")).toHaveLength(3);
    expect(within(container.querySelector(".acwr-trend-axis")).getByText("Bye")).toBeInTheDocument();
  });

  it("draws the four risk bands behind the line", () => {
    const { container } = render(<AcwrTrendChart games={SEASON} />);

    const bands = [...container.querySelectorAll(".acwr-trend-band")];
    expect(bands.map((b) => b.textContent)).toEqual(["Ramping up", "Normal", "Slightly elevated", "Spiking"]);
    expect(bands.map((b) => b.getAttribute("class"))).toEqual([
      "acwr-trend-band risk-medium",
      "acwr-trend-band risk-low",
      "acwr-trend-band risk-medium",
      "acwr-trend-band risk-high",
    ]);
  });

  it("shows the week, snaps and ACWR when hovering over a point", async () => {
    const user = userEvent.setup();
    const { container } = render(<AcwrTrendChart games={SEASON} />);

    await user.hover(screen.getByRole("img", { name: /^Week 7:/ }));

    expect(tooltip(container)).toEqual(["Week 7", "60 snaps", "ACWR 1.14"]);

    await user.unhover(screen.getByRole("img", { name: /^Week 7:/ }));

    expect(tooltip(container)).toBeNull();
  });

  it("shows the week, snaps and ACWR when tabbing to a point", async () => {
    const user = userEvent.setup();
    const { container } = render(<AcwrTrendChart games={SEASON} />);

    await user.tab();
    expect(points()[0]).toHaveFocus();
    expect(tooltip(container)).toEqual(["Week 4", "50 snaps", "ACWR 1.00"]);

    await user.tab();
    expect(tooltip(container)).toEqual(["Week 6", "50 snaps", "ACWR 1.00"]);
  });

  it("says the trend over the last 3 weeks under the chart", () => {
    render(<AcwrTrendChart games={SEASON} />);

    expect(screen.getByText("Rising")).toBeInTheDocument();
  });

  it("has a table of every week for screen readers, including the bye", () => {
    render(<AcwrTrendChart games={SEASON} />);

    const table = screen.getByRole("table", { name: "ACWR by week" });
    const rows = within(table).getAllByRole("row").slice(1);
    expect(rows.map((row) => row.textContent)).toEqual([
      "150—No data",
      "250—No data",
      "350—No data",
      "4501.00Normal workload",
      "5Bye—Bye week",
      "6501.00Normal workload",
      "7601.14Normal workload",
      "8701.22Normal workload",
    ]);
  });

  it("shows No trend data yet for a player with no history", () => {
    render(<AcwrTrendChart games={[]} />);

    expect(screen.getByText("No trend data yet.")).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
  });

  it("shows No trend data yet for a player with fewer than 4 games", () => {
    render(<AcwrTrendChart games={games([50, 60, 70])} />);

    expect(screen.getByText("No trend data yet.")).toBeInTheDocument();
  });

  it("is labeled Sample data", () => {
    render(<AcwrTrendChart games={SEASON} />);

    expect(screen.getByText("Sample data")).toBeInTheDocument();
  });

  it("says it's loading, or why it couldn't load", () => {
    const { rerender } = render(<AcwrTrendChart games={null} />);
    expect(screen.getByText("Loading workload history…")).toBeInTheDocument();

    rerender(<AcwrTrendChart games={null} error="GET /players/1/workload failed with status 500" />);
    expect(
      screen.getByText("Couldn't load workload history: GET /players/1/workload failed with status 500"),
    ).toBeInTheDocument();
  });
});
