import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
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

// Types a query into the search box and presses Enter to fill the results grid.
function submitSearch(text) {
  const searchBox = screen.getByRole("combobox", { name: "Search players by name" });
  fireEvent.change(searchBox, { target: { value: text } });
  fireEvent.keyDown(searchBox, { key: "Enter" });
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

describe("homepage animation", () => {
  it("renders the animated menu and workload pulse on load", async () => {
    await renderHome();

    expect(
      screen.getByRole("button", { name: "Toggle navigation menu" }).closest(".fade-in"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("workload-pulse")).toBeInTheDocument();
  });

  it("animates the title in, while keeping its name readable as one word", async () => {
    await renderHome();

    expect(
      screen.getByRole("heading", { level: 1, name: "SIDELINED" }),
    ).toBeInTheDocument();
    expect(screen.getAllByTestId("staggered-text")).toHaveLength(2);
  });

  it("holds the bars back for the title on first load, but not after returning home", async () => {
    const user = userEvent.setup();
    await renderHome();
    const firstBarDelay = () =>
      screen
        .getByTestId("workload-pulse")
        .querySelector(".workload-pulse-bar").style.animationDelay;
    expect(firstBarDelay()).toBe("1300ms");

    await user.click(
      screen.getByRole("button", { name: "Toggle navigation menu" }),
    );
    await user.click(screen.getByRole("menuitem", { name: "About" }));
    await user.click(screen.getByText("← Back to home"));

    expect(firstBarDelay()).toBe("0ms");
  });

  it("fills the homepage with the backdrop and feature cards showing live counts", async () => {
    savedEntries = [{ id: 1, playerId: 7, note: "" }];
    await renderHome(PLAYERS);

    expect(screen.getByTestId("home-backdrop")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Roster/ })).toHaveTextContent(
      "Browse all 1 teams",
    );
    expect(screen.getByRole("button", { name: /Search/ })).toHaveTextContent(
      "Find any of 1 players",
    );
    expect(
      await screen.findByRole("button", { name: /On your list: 1/ }),
    ).toBeInTheDocument();
  });

  it("opens a page from its feature card on the first click", async () => {
    const user = userEvent.setup();
    await renderHome();

    await user.click(screen.getByRole("button", { name: /Watchlist/ }));

    expect(screen.getByRole("heading", { name: "Watchlist" })).toBeInTheDocument();
    expect(screen.queryByTestId("home-backdrop")).not.toBeInTheDocument();
  });

  it("doesn't delay the menu: it opens on the first click after load", async () => {
    const user = userEvent.setup();
    await renderHome();

    await user.click(
      screen.getByRole("button", { name: "Toggle navigation menu" }),
    );

    expect(screen.getByRole("menuitem", { name: "Roster" })).toBeInTheDocument();
  });
});

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

  it("does not show the What is ACWR? link on the homepage", async () => {
    await renderHome();

    expect(
      screen.queryByRole("button", { name: /what is acwr/i }),
    ).not.toBeInTheDocument();
  });

  it("reaches the ACWR info page from a link on the About page", async () => {
    const user = userEvent.setup();
    await renderHome();

    await user.click(
      screen.getByRole("button", { name: "Toggle navigation menu" }),
    );
    await user.click(screen.getByRole("menuitem", { name: "About" }));
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
  submitSearch("brissett");

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
    submitSearch("brissett");

    // Assert
    expect(screen.getByText("Add to Watchlist")).toBeEnabled();
  });
});

describe("search persistence", () => {
  const SEARCH_PLAYERS = [
    { id: 1, name: "CeeDee Lamb", team: "DAL", position: "WR", snapsLastGame: 60, snapsLast4Games: 230 },
    { id: 2, name: "Dak Prescott", team: "DAL", position: "QB", snapsLastGame: 68, snapsLast4Games: 253 },
    { id: 3, name: "A.J. Brown", team: "PHI", position: "WR", snapsLastGame: 58, snapsLast4Games: 220 },
  ];

  async function openSearchPage(user) {
    await user.click(screen.getByRole("button", { name: "Toggle navigation menu" }));
    await user.click(screen.getByRole("menuitem", { name: "Search" }));
  }

  function chip(group, label) {
    return within(screen.getByRole("group", { name: group })).getByRole("button", { name: label });
  }

  it("keeps the query and filters after leaving search and coming back", async () => {
    const user = userEvent.setup();
    await renderHome(SEARCH_PLAYERS);

    await openSearchPage(user);
    await user.type(screen.getByRole("combobox", { name: "Search players by name" }), "la");
    await user.click(chip("Position", "WR"));
    await user.click(chip("Team", "DAL"));
    await user.click(screen.getByText("← Back to home"));
    await openSearchPage(user);

    expect(screen.getByRole("combobox", { name: "Search players by name" })).toHaveValue("la");
    expect(chip("Position", "WR")).toHaveAttribute("aria-pressed", "true");
    expect(chip("Team", "DAL")).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByText("CeeDee Lamb")).toBeInTheDocument();
    expect(screen.queryByText("A.J. Brown")).not.toBeInTheDocument();
  });

  it("starts empty again after Reset, even after leaving and coming back", async () => {
    const user = userEvent.setup();
    await renderHome(SEARCH_PLAYERS);

    await openSearchPage(user);
    await user.click(chip("Team", "PHI"));
    await user.click(screen.getByRole("button", { name: "Reset" }));
    await user.click(screen.getByText("← Back to home"));
    await openSearchPage(user);

    expect(chip("Team", "PHI")).toHaveAttribute("aria-pressed", "false");
    expect(screen.queryAllByRole("heading", { level: 2 })).toHaveLength(0);
    expect(screen.getByText("Search a name or pick a position/team.")).toBeInTheDocument();
  });
});

describe("opening a player and going back", () => {
  async function openMenuItem(user, label) {
    await user.click(screen.getByRole("button", { name: "Toggle navigation menu" }));
    await user.click(screen.getByRole("menuitem", { name: label }));
  }

  it("returns to search, with the query kept, when the player was opened from search", async () => {
    const user = userEvent.setup();
    await renderHome(PLAYERS);

    await openMenuItem(user, "Search");
    await user.type(screen.getByRole("combobox", { name: "Search players by name" }), "bri");
    await user.keyboard("{ArrowDown}{Enter}");
    expect(screen.getByRole("heading", { level: 1, name: "Jacoby Brissett" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Back to search" }));

    expect(screen.getByRole("heading", { name: "Search Players" })).toBeInTheDocument();
    expect(screen.getByRole("combobox", { name: "Search players by name" })).toHaveValue("bri");
  });

  it("returns to the team roster when the player was opened from the formation", async () => {
    const user = userEvent.setup();
    await renderHome(PLAYERS);

    await openMenuItem(user, "Roster");
    await user.click(screen.getByRole("button", { name: "ARI" }));
    await user.click(screen.getByRole("heading", { name: "Jacoby Brissett" }));
    expect(screen.getByRole("heading", { level: 1, name: "Jacoby Brissett" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Back to team page" }));

    expect(screen.getByRole("heading", { level: 1, name: "ARI" })).toBeInTheDocument();
  });

  it("uses the latest origin after opening players from both places", async () => {
    const user = userEvent.setup();
    await renderHome(PLAYERS);

    await openMenuItem(user, "Roster");
    await user.click(screen.getByRole("button", { name: "ARI" }));
    await user.click(screen.getByRole("heading", { name: "Jacoby Brissett" }));
    await user.click(screen.getByRole("button", { name: "Back to team page" }));
    await user.click(screen.getByRole("button", { name: "Back to teams" }));
    await user.click(screen.getByRole("button", { name: "Back to home" }));

    await openMenuItem(user, "Search");
    await user.type(screen.getByRole("combobox", { name: "Search players by name" }), "jacoby{Enter}");
    await user.click(screen.getByRole("heading", { level: 2, name: "Jacoby Brissett" }));
    await user.click(screen.getByRole("button", { name: "Back to search" }));

    expect(screen.getByRole("heading", { name: "Search Players" })).toBeInTheDocument();
  });
});
