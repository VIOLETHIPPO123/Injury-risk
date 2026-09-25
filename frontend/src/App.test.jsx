import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, test, vi } from "vitest";
import App from "./App";
import { getPlayers } from "./services/playerService";

vi.mock("./services/playerService", () => ({
  getPlayers: vi.fn(),
}));

const PLAYERS = [
  {
    id: 7,
    name: "Jacoby Brissett",
    team: "ARI",
    position: "QB",
    snapsLastGame: 68,
    snapsLast4Games: 253,
  },
];

function jsonResponse(status, body) {
  return Promise.resolve({
    ok: true,
    status,
    json: () => Promise.resolve(body),
  });
}

// A fake backend for the watchlist: answers GET /watchlist and POST /watchlist.
// Players come from the mocked playerService above.
beforeEach(() => {
  vi.stubGlobal(
    "fetch",
    vi.fn((url, options = {}) => {
      if (url.endsWith("/watchlist") && options.method === "POST") {
        const { playerId, note } = JSON.parse(options.body);
        return jsonResponse(201, { id: 1, playerId, note });
      }
      if (url.endsWith("/watchlist")) return jsonResponse(200, []);
      throw new Error(`Unexpected request: ${url}`);
    }),
  );
});

afterEach(() => {
  vi.unstubAllGlobals();
});

async function renderHome(players = []) {
  getPlayers.mockResolvedValue(players);
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
    ["Watchlist", "Watchlist"],
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

test("Add to Watchlist submits the expected payload and the player shows up on the watchlist page", async () => {
  // Arrange
  await renderHome(PLAYERS);
  fireEvent.click(screen.getByRole("button", { name: "Toggle navigation menu" }));
  fireEvent.click(screen.getByRole("menuitem", { name: "Search" }));

  // Act
  fireEvent.click(screen.getByText("Add to Watchlist"));

  // Assert — the request sent to the backend
  const postCall = fetch.mock.calls.find(([, opts]) => opts?.method === "POST");
  expect(postCall[0]).toBe("http://localhost:8080/watchlist");
  expect(postCall[1].headers).toEqual({ "Content-Type": "application/json" });
  expect(JSON.parse(postCall[1].body)).toEqual({ playerId: 7, note: "" });

  // Assert — the UI reflects the new entry
  expect(await screen.findByText("On watchlist")).toBeDisabled();
  fireEvent.click(screen.getByText("← Back to home"));
  fireEvent.click(screen.getByRole("button", { name: "Toggle navigation menu" }));
  fireEvent.click(screen.getByRole("menuitem", { name: "Watchlist" }));
  expect(screen.getByText("Jacoby Brissett")).toBeInTheDocument();
});
