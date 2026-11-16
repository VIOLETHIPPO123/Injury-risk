package com.sidelined.backend.repository;

import com.sidelined.backend.model.WorkloadEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

// The workload_entries table. Spring Data writes the implementation. WorkloadSyncJob fills it
// from nflverse; the services only see WorkloadRepository.
public interface JpaWorkloadRepository extends JpaRepository<WorkloadEntry, Long>, WorkloadRepository {

    // Only the newest season in the table, so last season's games drop off the trend chart
    // once this season's first snap counts are stored.
    @Override
    @Query("select e from WorkloadEntry e where e.playerId = :playerId"
        + " and e.season = (select max(s.season) from WorkloadEntry s) order by e.week")
    List<WorkloadEntry> findByPlayerId(@Param("playerId") Long playerId);

    // Every player's games in one season, for the sync job.
    List<WorkloadEntry> findBySeason(int season);
}
