// Positions that are compared against each other. Anything not listed is its own
// group: QB, WR, TE, P, LS, and the special-teams roles H, KR and PR.
const POSITION_GROUPS = {
  LT: "OL",
  LG: "OL",
  C: "OL",
  RG: "OL",
  RT: "OL",
  DE: "DL",
  DT: "DL",
  LB: "LB",
  CB: "DB",
  S: "DB",
  // The roster data splits safeties into free and strong.
  FS: "DB",
  SS: "DB",
  RB: "RB",
  FB: "RB",
  K: "K",
  // The roster data calls kickers "PK" (place kicker).
  PK: "K",
};

export function positionGroup(position) {
  return POSITION_GROUPS[position] ?? position;
}

// Watchlisted players in the same position group as `player`, never `player` itself.
export function compareCandidates(player, watchlistedPlayers) {
  const group = positionGroup(player.position);
  return watchlistedPlayers.filter(
    (other) =>
      other.id !== player.id && positionGroup(other.position) === group,
  );
}
