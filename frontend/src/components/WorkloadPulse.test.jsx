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

  it("ends on the tallest bar", () => {
    render(<WorkloadPulse />);
    const heights = bars().map((bar) => parseFloat(bar.style.height));

    expect(heights.at(-1)).toBe(100);
    expect(Math.max(...heights.slice(0, -1))).toBeLessThan(100);
  });

  it("staggers the bars after the given delay", () => {
    render(<WorkloadPulse delay={1300} />);

    expect(bars().map((bar) => bar.style.animationDelay)).toEqual(
      Array.from({ length: 12 }, (_, week) => `${1300 + week * 60}ms`),
    );
  });

  it("shows only the bars: no value label, legend or caption", () => {
    render(<WorkloadPulse />);

    expect(screen.getByTestId("workload-pulse")).toHaveTextContent("");
  });

  it("is hidden from screen readers because it is decoration", () => {
    render(<WorkloadPulse />);

    expect(screen.getByTestId("workload-pulse")).toHaveAttribute(
      "aria-hidden",
      "true",
    );
  });
});
