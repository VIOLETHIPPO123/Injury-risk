package com.sidelined.backend.repository;

import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.TeamDefense;
import org.springframework.stereotype.Repository;
import java.util.Arrays;
import java.util.List;

// One DEF entry per NFL team. Ids start at 100001, well clear of the player ids
// (1-940 today), so a roster can hold players and defenses side by side.
@Repository
public class InMemoryTeamDefenseRepository
        extends InMemoryReadOnlyRepository<TeamDefense, Long>
        implements TeamDefenseRepository {

    static final long ID_BASE = 100_000L;

    private static final List<TeamDefense> DEFENSES = Arrays.stream(NflTeam.values())
        .map(t -> new TeamDefense(ID_BASE + t.ordinal() + 1, t, t.getDisplayName() + " D/ST"))
        .toList();

    public InMemoryTeamDefenseRepository() {
        super(DEFENSES, TeamDefense::id);
    }
}
