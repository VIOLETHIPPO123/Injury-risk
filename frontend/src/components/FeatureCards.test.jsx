import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import FeatureCards from "./FeatureCards";

function items(overrides = {}) {
  return [
    { id: "roster", label: "Roster", detail: "Browse all 32 teams", onClick: vi.fn() },
    { id: "search", label: "Search", detail: "Find any of 900 players", onClick: vi.fn() },
    { id: "watchlist", label: "Watchlist", detail: "On your list: 2", onClick: vi.fn() },
    { id: "about", label: "About", detail: "What Sidelined is", onClick: vi.fn() },
  ].map((item) => ({ ...item, ...overrides[item.id] }));
}

describe("FeatureCards", () => {
  it("mounts one colored card per item, with its label and detail", () => {
    render(<FeatureCards items={items()} />);

    const roster = screen.getByRole("button", { name: /Roster/ });
    expect(roster).toHaveTextContent("Browse all 32 teams");
    expect(roster).toHaveClass("feature-card-roster");
    expect(screen.getAllByRole("button")).toHaveLength(4);
  });

  it("staggers the cards' entrance after the delay", () => {
    render(<FeatureCards items={items()} delay={500} />);

    expect(
      screen.getAllByRole("button").map((card) => card.style.animationDelay),
    ).toEqual(["500ms", "620ms", "740ms", "860ms"]);
  });

  it("is clickable straight away, before its entrance animation starts", async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    render(
      <FeatureCards items={items({ watchlist: { onClick } })} delay={5000} />,
    );

    await user.click(screen.getByRole("button", { name: /Watchlist/ }));

    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
