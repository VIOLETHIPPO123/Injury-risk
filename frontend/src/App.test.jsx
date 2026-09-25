import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import App from "./App";
import { getPlayers } from "./services/playerService";

vi.mock("./services/playerService", () => ({
  getPlayers: vi.fn(),
}));

describe("homepage navigation", () => {
  it("reaches the ACWR info page from a homepage button", async () => {
    getPlayers.mockResolvedValue([]);
    const user = userEvent.setup();
    render(<App />);

    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: /what is acwr/i }),
      ).toBeInTheDocument(),
    );

    await user.click(screen.getByRole("button", { name: /what is acwr/i }));

    expect(
      screen.getByRole("heading", { name: "Understanding ACWR" }),
    ).toBeInTheDocument();
  });
});
