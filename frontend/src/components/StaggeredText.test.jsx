import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import StaggeredText from "./StaggeredText";

function pieces() {
  return [...screen.getByTestId("staggered-text").querySelectorAll(".staggered-piece")];
}

describe("StaggeredText", () => {
  it("mounts and splits the text into one animated piece per letter", () => {
    render(<StaggeredText text="SIDELINED" />);

    expect(pieces().map((p) => p.textContent)).toEqual([..."SIDELINED"]);
    expect(pieces()[0]).toHaveClass("staggered-drop");
  });

  it("splits by word and keeps the spaces between words", () => {
    render(<StaggeredText text="Master the Game." by="word" variant="rise" />);

    expect(pieces().map((p) => p.textContent)).toEqual(["Master", "the", "Game."]);
    expect(screen.getByTestId("staggered-text")).toHaveTextContent("Master the Game.");
    expect(pieces()[0]).toHaveClass("staggered-rise");
  });

  it("starts each piece a step after the one before, after the initial delay", () => {
    render(<StaggeredText text="ABC" delay={600} step={100} />);

    expect(pieces().map((p) => p.style.animationDelay)).toEqual([
      "600ms",
      "700ms",
      "800ms",
    ]);
  });

  it("gives screen readers the whole text once, not letter by letter", () => {
    render(
      <h1>
        <StaggeredText text="SIDELINED" />
      </h1>,
    );

    expect(screen.getByRole("heading", { name: "SIDELINED" })).toBeInTheDocument();
    expect(screen.getByTestId("staggered-text")).toHaveAttribute("aria-hidden", "true");
  });
});
