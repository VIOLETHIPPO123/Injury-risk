import { Slot } from "./formation";

// One definition per unit. Adding a unit means adding a definition here, not new layout code.
// Rows are listed top to bottom; slots left to right. Missing players are skipped.

const OFFENSE = {
  rows: [
    {
      name: "top-row",
      slots: [
        Slot.pick("LT"),
        Slot.pick("LG"),
        Slot.pick("C"),
        Slot.pick("RG"),
        Slot.pick("RT"),
        Slot.pick("TE"),
      ],
    },
    {
      name: "bottom-row",
      slots: [
        Slot.pick("WR", 0),
        Slot.pick("WR", 1),
        Slot.pick("RB"),
        Slot.pick("QB"),
        Slot.pick("FB"),
        Slot.pick("WR", 2),
      ],
    },
  ],
};

const DEFENSE = {
  rows: [
    // Secondary: CB1, any nickel corners, FS, SS, CB2
    {
      name: "top-row",
      slots: [
        Slot.pick("CB", 0),
        Slot.from("CB", 2),
        Slot.pick("FS"),
        Slot.pick("SS"),
        Slot.pick("CB", 1),
      ],
    },
    // Linebackers: 3 (4-3) or 4 (3-4)
    { name: "middle-row", slots: [Slot.from("LB")] },
    // Defensive line: DE DT DT DE (4-3) or DE DT DE (3-4)
    {
      name: "bottom-row",
      slots: [Slot.centered(Slot.from("DE"), Slot.from("DT"))],
    },
  ],
};

const SPECIAL_TEAMS = {
  rows: [
    {
      name: "top-row",
      slots: [
        Slot.pick("PK"),
        Slot.pick("P"),
        Slot.pick("H"),
        Slot.pick("PR"),
        Slot.pick("KR"),
        Slot.pick("LS"),
      ],
    },
  ],
};

export { OFFENSE, DEFENSE, SPECIAL_TEAMS };
