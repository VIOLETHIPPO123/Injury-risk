package com.sidelined.backend.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// The league's chosen roster format, by RosterFormat id. The league has one, so the
// roster_settings table has one row, always id 1. In JSON it is just { "formatId": "..." }.
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Entity
@Table(name = "roster_settings")
public class RosterSettings {

    public static final Long ID = 1L;

    @JsonIgnore
    @Id
    private Long id = ID;

    @Column(name = "format_id", nullable = false, length = 32)
    private String formatId;

    // For JPA.
    protected RosterSettings() {
    }

    @JsonCreator
    public RosterSettings(@JsonProperty("formatId") String formatId) {
        this.formatId = formatId;
    }

    // What a new league starts on.
    public static RosterSettings defaults() {
        return new RosterSettings(StandardRosterFormats.ESPN_STANDARD);
    }

    public String formatId() { return formatId; }
}
