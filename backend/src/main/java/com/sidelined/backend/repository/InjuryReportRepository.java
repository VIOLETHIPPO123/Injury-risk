package com.sidelined.backend.repository;

import com.sidelined.backend.model.InjuryReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

// The injury_reports table. Only InjuryReportSyncJob writes to it; no controller exposes it.
// ReportedInjuryRepository reads it to work out injuries.
public interface InjuryReportRepository extends JpaRepository<InjuryReport, Long> {

    // Every stored report in these seasons, so the sync job can update them in place.
    List<InjuryReport> findBySeasonIn(Collection<Integer> seasons);

    // Every report for one player, in no particular order: the rows their injuries are made from.
    List<InjuryReport> findByPlayer_Id(Long playerId);

    // A report from the latest week anyone was reported: the current week's report.
    Optional<InjuryReport> findFirstByOrderBySeasonDescWeekDesc();
}
