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

// What GET /watchlist returns when the app loads. Tests that start with entries overwrite it.
let savedEntries = [];

// A fake backend for the watchlist CRUD endpoints. Players come from the mocked playerService above.
beforeEach(() => {
  savedEntries = [];
  vi.stubGlobal(
    "fetch",
    vi.fn((url, options = {}) => {
      const method = options.method ?? "GET";
      const id = Number(url.match(/\/watchlist\/(\d+)$/)?.[1]);

      if (url.endsWith("/watchlist") && method === "POST") {
        const { playerId, note } = JSON.parse(options.body);
        return jsonResponse(201, { id: 1, playerId, note });
      }
      if (url.endsWith("/watchlist")) return jsonResponse(200, savedEntries);
      if (id && method === "PUT") {
        const existing = savedEntries.find((e) => e.id === id);
        return jsonResponse(200, { ...existing, ...JSON.parse(options.body) });
      }
      if (id && method === "DELETE") return jsonResponse(204);
      throw new Error(`Unexpected request: ${method} ${url}`);
    }),
  );
});

function watchlistGetCalls() {
  return fetch.mock.calls.filter(
    ([url, opts]) => url.endsWith("/watchlist") && opts?.method === "GET",
  );
}

async function openWatchlistPage() {
  fireEvent.click(screen.getByRole("button", { name: "Toggle navigation menu" }));
  fireEvent.click(screen.getByRole("menuitem", { name: "Watchlist" }));
}

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
  await openWatchlistPage();
  expect(screen.getByText("Jacoby Brissett")).toBeInTheDocument();
  // The new entry came from the POST response; the list was never reloaded.
  expect(watchlistGetCalls()).toHaveLength(1);
});

describe("watchlist updates without a page refresh", () => {
  test("editing a note shows the new note right away", async () => {
    // Arrange
    savedEntries = [{ id: 1, playerId: 7, note: "Monitor snaps" }];
    await renderHome(PLAYERS);
    await openWatchlistPage();
    expect(await screen.findByText("Monitor snaps")).toBeInTheDocument();

    // Act
    fireEvent.click(screen.getByText("Edit"));
    fireEvent.change(screen.getByLabelText("Note"), {
      target: { value: "Snap share dropping" },
    });
    fireEvent.click(screen.getByText("Save"));

    // Assert — wait for the form to close, since the open textarea also contains the draft text.
    expect(await screen.findByText("Edit")).toBeInTheDocument();
    expect(screen.getByText("Snap share dropping")).toBeInTheDocument();
    expect(screen.queryByText("Monitor snaps")).not.toBeInTheDocument();
    expect(watchlistGetCalls()).toHaveLength(1);
  });

  test("removing an entry takes it off the list right away", async () => {
    // Arrange
    savedEntries = [{ id: 1, playerId: 7, note: "Monitor snaps" }];
    await renderHome(PLAYERS);
    await openWatchlistPage();
    expect(await screen.findByText("Jacoby Brissett")).toBeInTheDocument();

    // Act
    fireEvent.click(screen.getByText("Remove"));

    // Assert
    expect(await screen.findByText(/Your watchlist is empty/)).toBeInTheDocument();
    expect(screen.queryByText("Jacoby Brissett")).not.toBeInTheDocument();
    expect(watchlistGetCalls()).toHaveLength(1);
  });

  test("removing an entry re-enables Add to Watchlist for that player", async () => {
    // Arrange
    savedEntries = [{ id: 1, playerId: 7, note: "" }];
    await renderHome(PLAYERS);
    await openWatchlistPage();
    fireEvent.click(await screen.findByText("Remove"));
    await screen.findByText(/Your watchlist is empty/);

    // Act
    fireEvent.click(screen.getByText("← Back to home"));
    fireEvent.click(screen.getByRole("button", { name: "Toggle navigation menu" }));
    fireEvent.click(screen.getByRole("menuitem", { name: "Search" }));

    // Assert
    expect(screen.getByText("Add to Watchlist")).toBeEnabled();
  });
});
