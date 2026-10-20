import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import WorkloadPulse from "./WorkloadPulse";

function bars() {
  return [
    ...screen
      .getByTestId("workload-pulse")
      .querySelectorAll(".workload-pulse-bar"),
  ];
}

function tierOf(bar) {
  return [...bar.classList].find((c) => c.startsWith("risk-"));
}

describe("WorkloadPulse", () => {
  it("mounts and renders twelve weekly bars", () => {
    render(<WorkloadPulse />);

    expect(bars()).toHaveLength(12);
  });

  it("colors each week by its ACWR tier, scored against the four weeks before it", () => {
    render(<WorkloadPulse />);

    expect(bars().map(tierOf)).toEqual([
      "risk-unknown",
      "risk-unknown",
      "risk-unknown",
      "risk-unknown",
      "risk-low",
      "risk-low",
      "risk-low",
      "risk-low",
      "risk-low",
      "risk-low",
      "risk-medium",
      "risk-high",
    ]);
  });

  it("ends on the tallest bar, labelled with its ACWR", () => {
    render(<WorkloadPulse />);
    const heights = bars().map((bar) => parseFloat(bar.style.height));

    expect(heights.at(-1)).toBe(100);
    expect(Math.max(...heights.slice(0, -1))).toBeLessThan(100);
    // 88 snaps against a 55.5 four-week average.
    expect(bars().at(-1)).toHaveTextContent("1.59");
  });

  it("staggers the bars after the given delay", () => {
    render(<WorkloadPulse delay={1300} />);

    expect(bars().map((bar) => bar.style.animationDelay)).toEqual(
      Array.from({ length: 12 }, (_, week) => `${1300 + week * 60}ms`),
    );
  });

  it("explains the colors with a legend and a caption", () => {
    render(<WorkloadPulse />);

    for (const label of ["Not enough history", "Normal", "Caution", "High risk"]) {
      expect(screen.getByText(label)).toBeInTheDocument();
    }
    expect(screen.getByRole("figure")).toHaveTextContent("spike to ACWR 1.59");
  });

  it("hides the bars themselves from screen readers", () => {
    render(<WorkloadPulse />);

    expect(bars()[0].parentElement).toHaveAttribute("aria-hidden", "true");
  });
});
