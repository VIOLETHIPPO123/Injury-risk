package com.sidelined.backend;

import com.sidelined.backend.exception.NoActiveInjuryException;
import com.sidelined.backend.model.Injury;
import com.sidelined.backend.model.InjuryReport;
import com.sidelined.backend.model.InjuryStatus;
import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.repository.InjuryReportRepository;
import com.sidelined.backend.repository.InjuryRepository;
import com.sidelined.backend.repository.JpaPlayerRepository;
import com.sidelined.backend.validation.InjuredReserveValidator;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Injuries worked out from real injury_reports rows (the in-memory H2 database of the "test"
// profile), and the IR rule that uses them. Each test runs in a transaction that is rolled back.
// The current week is the latest one in the table, so the reports use a season far in the
// future to be the latest whatever else other tests have stored.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InjuryHistoryPersistenceTest {

    private static final int SEASON = 2099;
    private static final String USER = "user-a";

    @Autowired private InjuryRepository injuryRepository;
    @Autowired private InjuryReportRepository injuryReportRepository;
    @Autowired private JpaPlayerRepository playerRepository;
    @Autowired private InjuredReserveValidator injuredReserveValidator;
    @Autowired private EntityManager entityManager;

    private Player hurt;
    private Player questionable;
    private Player healthy;

    private Player player(String gsisId, String name) {
        return playerRepository.save(new Player(null, gsisId, name, NflTeam.PHI, Position.WR, new Workload(0, 0)));
    }

    private void report(Player player, int week, String injury, String gameStatus) {
        injuryReportRepository.save(new InjuryReport(player, SEASON, week, injury, null, gameStatus, null));
    }

    private static RosterEntry entryFor(Player player) {
        RosterEntry entry = mock(RosterEntry.class);
        when(entry.getPlayerId()).thenReturn(player.getId());
        return entry;
    }

    // Week 3 is the current week.
    @BeforeEach
    void storeReports() {
        hurt = player("00-HIST-01", "Out This Week");
        questionable = player("00-HIST-02", "Questionable This Week");
        healthy = player("00-HIST-03", "Hurt Last Season");

        report(hurt, 1, "Knee", "Out");
        report(hurt, 3, "Knee", "Out");
        report(questionable, 3, "Ankle", "Questionable");
        injuryReportRepository.save(new InjuryReport(healthy, SEASON - 1, 4, "Hamstring", null, "Out", null));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void reportsAroundAByeWeek_areOneActiveInjury() {
        List<Injury> injuries = injuryRepository.findByPlayerId(hurt.getId());

        assertEquals(1, injuries.size());
        Injury knee = injuries.get(0);
        assertEquals(SEASON, knee.season());
        assertEquals(1, knee.week());
        assertEquals("Knee", knee.bodyPart());
        assertEquals(2, knee.gamesMissed());
        assertEquals(InjuryStatus.ACTIVE, knee.status());
    }

    @Test
    void statuses_followTheCurrentWeeksReport() {
        assertEquals(InjuryStatus.RECOVERING, injuryRepository.findByPlayerId(questionable.getId()).get(0).status());
        assertEquals(InjuryStatus.RECOVERED, injuryRepository.findByPlayerId(healthy.getId()).get(0).status());
    }

    @Test
    void playerWithNoReports_hasNoInjuries() {
        Player neverHurt = player("00-HIST-04", "Never Hurt");

        assertEquals(List.of(), injuryRepository.findByPlayerId(neverHurt.getId()));
        assertFalse(injuryRepository.hasActiveInjury(neverHurt.getId()));
    }

    @Test
    void ir_takesOnlyAPlayerOutOrDoubtfulThisWeek() {
        assertDoesNotThrow(() -> injuredReserveValidator.validate(USER, entryFor(hurt), RosterEntry.IR));
        assertThrows(NoActiveInjuryException.class,
            () -> injuredReserveValidator.validate(USER, entryFor(questionable), RosterEntry.IR));
        assertThrows(NoActiveInjuryException.class,
            () -> injuredReserveValidator.validate(USER, entryFor(healthy), RosterEntry.IR));
    }
}
