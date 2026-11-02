package com.sidelined.backend.model;

import com.fasterxml.jackson.annotation.JsonValue;

// Where an injury stands now. Only an ACTIVE injury lets a player go on IR.
public enum InjuryStatus {
    ACTIVE("Active"),
    RECOVERING("Recovering"),
    RECOVERED("Recovered");

    private final String label;

    InjuryStatus(String label) {
        this.label = label;
    }

    // The JSON says "Active", not "ACTIVE", so the injury table shows it as is.
    @JsonValue
    public String getLabel() { return label; }
}
