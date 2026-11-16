package com.sidelined.backend.repository;

import com.sidelined.backend.model.Injury;
import com.sidelined.backend.model.InjuryHistory;
import com.sidelined.backend.model.InjuryReport;
import com.sidelined.backend.model.InjuryStatus;
import org.springframework.stereotype.Repository;
import java.util.List;

// Injuries worked out, on each request, from the player's rows in injury_reports (filled daily
// by InjuryReportSyncJob). The rules are in InjuryHistory.
@Repository
public class ReportedInjuryRepository implements InjuryRepository {

    private final InjuryReportRepository injuryReportRepository;

    public ReportedInjuryRepository(InjuryReportRepository injuryReportRepository) {
        this.injuryReportRepository = injuryReportRepository;
    }

    @Override
    public List<Injury> findByPlayerId(Long playerId) {
        List<InjuryReport> reports = injuryReportRepository.findByPlayer_Id(playerId);
        if (reports.isEmpty()) {
            return List.of();
        }
        InjuryHistory.ReportWeek current = injuryReportRepository.findFirstByOrderBySeasonDescWeekDesc()
            .map(latest -> new InjuryHistory.ReportWeek(latest.getSeason(), latest.getWeek()))
            .orElse(null);
        return InjuryHistory.of(playerId, reports, current);
    }

    @Override
    public boolean hasActiveInjury(Long playerId) {
        return findByPlayerId(playerId).stream().anyMatch(injury -> injury.status() == InjuryStatus.ACTIVE);
    }
}
