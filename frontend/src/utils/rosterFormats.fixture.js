// For tests: what GET /roster/formats returns (see InMemoryRosterFormatRepository on the backend).
import { toLineupFormat } from "./rosterFormats";

const CORE = [
  { name: "QB", count: 1, positions: ["QB"] },
  { name: "RB", count: 2, positions: ["RB"] },
  { name: "WR", count: 2, positions: ["WR"] },
  { name: "TE", count: 1, positions: ["TE"] },
  { name: "K", count: 1, positions: ["K"] },
  { name: "DEF", count: 1, positions: ["DEF"] },
];

export const API_ROSTER_FORMATS = [
  {
    id: "espn-standard",
    name: "ESPN Standard",
    slots: [...CORE, { name: "FLEX", count: 1, positions: ["RB", "WR", "TE"] }],
    benchCount: 7,
    irCount: 1,
    maxRosterSize: 16,
  },
  {
    id: "yahoo-standard",
    name: "Yahoo Standard",
    slots: [...CORE, { name: "FLEX", count: 1, positions: ["RB", "WR", "TE"] }],
    benchCount: 6,
    irCount: 2,
    maxRosterSize: 15,
  },
  {
    id: "sleeper-default",
    name: "Sleeper Default",
    slots: [...CORE, { name: "FLEX", count: 2, positions: ["WR", "RB", "TE"] }],
    benchCount: 5,
    irCount: 0,
    maxRosterSize: 15,
  },
];

export const LINEUP_FORMATS = API_ROSTER_FORMATS.map(toLineupFormat);

export const lineupFormat = (id) => LINEUP_FORMATS.find((f) => f.id === id);
