import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import HomeBackdrop from "./HomeBackdrop";

describe("HomeBackdrop", () => {
  it("mounts the glow and a turf band with yard numbers up to the 50", () => {
    render(<HomeBackdrop />);
    const backdrop = screen.getByTestId("home-backdrop");

    expect(backdrop.querySelector(".home-glow")).toBeInTheDocument();
    expect(
      [...backdrop.querySelectorAll(".home-turf-numbers span")].map(
        (n) => n.textContent,
      ),
    ).toEqual(["10", "20", "30", "40", "50", "40", "30", "20", "10"]);
  });

  it("is hidden from screen readers because it is decoration", () => {
    render(<HomeBackdrop />);

    expect(screen.getByTestId("home-backdrop")).toHaveAttribute(
      "aria-hidden",
      "true",
    );
  });
});
