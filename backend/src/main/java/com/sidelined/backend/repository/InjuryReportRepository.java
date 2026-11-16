package com.sidelined.backend.repository;

import com.sidelined.backend.model.InjuryReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

// The injury_reports table. Only InjuryReportSyncJob writes to it; no controller exposes it.
public interface InjuryReportRepository extends JpaRepository<InjuryReport, Long> {

    // Every stored report in these seasons, so the sync job can update them in place.
    List<InjuryReport> findBySeasonIn(Collection<Integer> seasons);
}
