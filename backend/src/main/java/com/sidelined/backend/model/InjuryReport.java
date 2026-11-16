package com.sidelined.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

// One player's line on one week's official NFL injury report, as nflverse publishes it: the raw
// data the injury history and risk will be worked out from. InjuryReportSyncJob fills the table;
// the API never writes to it. A player has at most one row per week (the unique constraint).
@Entity
@Table(name = "injury_reports",
    uniqueConstraints = @UniqueConstraint(
        name = "injury_reports_player_season_week", columnNames = {"player_id", "season", "week"}))
public class InjuryReport {

    // Values longer than this are cut, so one odd row can't make the whole save fail.
    private static final int MAX_TEXT = 255;

    // A sequence rather than IDENTITY, so Hibernate can send new rows in batches (see
    // hibernate.jdbc.batch_size): the first load is about 15,000 rows.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "injury_reports_seq")
    @SequenceGenerator(name = "injury_reports_seq", sequenceName = "injury_reports_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private int season;

    // 1 to 18 is the regular season; 19 to 22 are the playoffs.
    @Column(nullable = false)
    private int week;

    // Free text from the NFL ("Knee", "Not injury related - resting"); null when not given.
    @Column(name = "primary_injury", length = MAX_TEXT)
    private String primaryInjury;

    @Column(name = "secondary_injury", length = MAX_TEXT)
    private String secondaryInjury;

    // "Out", "Doubtful", "Questionable"; null when the player was only on the practice report.
    @Column(name = "game_status", length = MAX_TEXT)
    private String gameStatus;

    // e.g. "Limited Participation in Practice".
    @Column(name = "practice_status", length = MAX_TEXT)
    private String practiceStatus;

    // For JPA.
    protected InjuryReport() {
    }

    public InjuryReport(Player player, int season, int week, String primaryInjury, String secondaryInjury,
                        String gameStatus, String practiceStatus) {
        this.player = player;
        this.season = season;
        this.week = week;
        update(primaryInjury, secondaryInjury, gameStatus, practiceStatus);
    }

    // Takes nflverse's latest values for this player and week. Returns whether anything changed,
    // so the sync job saves only the rows that did.
    public boolean update(String primaryInjury, String secondaryInjury, String gameStatus, String practiceStatus) {
        String primary = fit(primaryInjury);
        String secondary = fit(secondaryInjury);
        String game = fit(gameStatus);
        String practice = fit(practiceStatus);
        boolean changed = !Objects.equals(primary, this.primaryInjury)
            || !Objects.equals(secondary, this.secondaryInjury)
            || !Objects.equals(game, this.gameStatus)
            || !Objects.equals(practice, this.practiceStatus);
        this.primaryInjury = primary;
        this.secondaryInjury = secondary;
        this.gameStatus = game;
        this.practiceStatus = practice;
        return changed;
    }

    private static String fit(String text) {
        return text == null || text.length() <= MAX_TEXT ? text : text.substring(0, MAX_TEXT);
    }

    public Long getId()               { return id; }
    public Player getPlayer()         { return player; }
    public int getSeason()            { return season; }
    public int getWeek()              { return week; }
    public String getPrimaryInjury()  { return primaryInjury; }
    public String getSecondaryInjury() { return secondaryInjury; }
    public String getGameStatus()     { return gameStatus; }
    public String getPracticeStatus() { return practiceStatus; }
}
