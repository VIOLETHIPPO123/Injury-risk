package com.sidelined.backend.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// One user's chosen roster format, by RosterFormat id. Each user has at most one row (profile_id
// is unique); a user without one is on the default format. In JSON it is just { "formatId": "..." }.
// The table is user_roster_settings: the old single-row roster_settings table (id 1) couldn't
// become per-user in place, since Hibernate's "update" mode can't change a table's primary key.
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Entity
@Table(name = "user_roster_settings")
public class RosterSettings {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Whose settings these are. Never in the JSON: the owner always comes from the sign-in token.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private Profile profile;

    @Column(name = "format_id", nullable = false, length = 32)
    private String formatId;

    // For JPA.
    protected RosterSettings() {
    }

    // A request body ({ "formatId": "..." }), or the unsaved defaults. It has no owner,
    // so it can't be saved: RosterService saves the user's own row instead.
    @JsonCreator
    public RosterSettings(@JsonProperty("formatId") String formatId) {
        this.formatId = formatId;
    }

    // A user's settings row, ready to save. The database assigns the id.
    public RosterSettings(Profile profile, String formatId) {
        this.profile = profile;
        this.formatId = formatId;
    }

    // What a new user starts on.
    public static RosterSettings defaults() {
        return new RosterSettings(StandardRosterFormats.ESPN_STANDARD);
    }

    public String formatId() { return formatId; }

    public Profile profile() { return profile; }

    public void changeFormat(String formatId) {
        this.formatId = formatId;
    }
}
