import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import App from "./App";
import { getPlayers } from "./services/playerService";

vi.mock("./services/playerService", () => ({
  getPlayers: vi.fn(),
}));

async function renderHome() {
  getPlayers.mockResolvedValue([]);
  render(<App />);
  await waitFor(() =>
    expect(
      screen.getByRole("button", { name: "Toggle navigation menu" }),
    ).toBeInTheDocument(),
  );
}

describe("homepage navigation", () => {
  it.each([
    ["Roster", "Rosters"],
    ["Search", "Search Players"],
    ["About", "About Sidelined"],
  ])(
    "renders a %s item in the nav menu that navigates to the %s page",
    async (label, pageHeading) => {
      const user = userEvent.setup();
      await renderHome();

      await user.click(
        screen.getByRole("button", { name: "Toggle navigation menu" }),
      );
      await user.click(screen.getByRole("menuitem", { name: label }));

      expect(
        screen.getByRole("heading", { name: pageHeading }),
      ).toBeInTheDocument();
    },
  );

  it("reaches the ACWR info page from a homepage button", async () => {
    const user = userEvent.setup();
    await renderHome();

    await user.click(screen.getByRole("button", { name: /what is acwr/i }));

    expect(
      screen.getByRole("heading", { name: "Understanding ACWR" }),
    ).toBeInTheDocument();
  });
});
