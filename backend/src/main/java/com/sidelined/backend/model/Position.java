package com.sidelined.backend.model;

public enum Position {
    // Offense
    QB(Group.OFFENSE),
    RB(Group.OFFENSE),
    FB(Group.OFFENSE),
    WR(Group.OFFENSE),
    TE(Group.OFFENSE),
    LT(Group.OFFENSE),
    LG(Group.OFFENSE),
    C(Group.OFFENSE),
    RG(Group.OFFENSE),
    RT(Group.OFFENSE),

    // Defense
    DE(Group.DEFENSE),
    DT(Group.DEFENSE),
    LB(Group.DEFENSE),
    CB(Group.DEFENSE),
    SS(Group.DEFENSE),
    FS(Group.DEFENSE),

    // Special Teams
    PK(Group.SPECIAL_TEAMS),
    P(Group.SPECIAL_TEAMS),
    H(Group.SPECIAL_TEAMS),
    PR(Group.SPECIAL_TEAMS),
    KR(Group.SPECIAL_TEAMS),
    LS(Group.SPECIAL_TEAMS);

    public enum Group {
        OFFENSE,
        DEFENSE,
        SPECIAL_TEAMS
    }

    private final Group group;

    Position(Group group) {
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }
}