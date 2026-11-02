package com.sidelined.backend.repository;

import com.sidelined.backend.model.Injury;
import com.sidelined.backend.model.InjuryStatus;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sidelined.backend.model.InjuryStatus.*;

// Hard-coded stand-in until the external API module. These injuries are SAMPLE DATA, made up
// to exercise the injury history view: the players are real, the injuries are not.
// The lookups by id live in InMemoryReadOnlyRepository; this class adds the per-player ones.
@Repository
public class InMemoryInjuryRepository
        extends InMemoryReadOnlyRepository<Injury, Long>
        implements InjuryRepository {

    private static final List<Injury> INJURIES = List.of(
        // Christian McCaffrey: a repeated hamstring injury.
        new Injury(1L, 321L, 2023, 5, "Hamstring", "Strain", 1, RECOVERED),
        new Injury(2L, 321L, 2024, 1, "Achilles", "Tendinitis", 8, RECOVERED),
        new Injury(3L, 321L, 2025, 9, "Hamstring", "Strain", 2, RECOVERED),
        new Injury(4L, 321L, 2026, 6, "Calf", "Strain", 1, RECOVERING),
        // Bijan Robinson: a repeated knee injury.
        new Injury(5L, 13L, 2025, 14, "Knee", "Sprain", 1, RECOVERED),
        new Injury(6L, 13L, 2026, 7, "Knee", "Bone bruise", 0, RECOVERING),
        // Ja'Marr Chase
        new Injury(7L, 69L, 2024, 12, "Hip", "Contusion", 0, RECOVERED),
        new Injury(8L, 69L, 2025, 3, "Ankle", "Sprain", 2, RECOVERED),
        // CeeDee Lamb
        new Injury(9L, 92L, 2026, 4, "Ankle", "Sprain", 1, RECOVERING),
        // Travis Kelce
        new Injury(10L, 173L, 2023, 1, "Knee", "Hyperextension", 1, RECOVERED),
        // Javonte Williams
        new Injury(11L, 91L, 2022, 4, "Knee", "Torn ACL", 13, RECOVERED),
        new Injury(12L, 91L, 2025, 10, "Hamstring", "Strain", 1, RECOVERED),
        // Mark Andrews and Tee Higgins: hurt now (ACTIVE), so they can go on IR.
        new Injury(13L, 28L, 2026, 8, "Ankle", "High sprain", 0, ACTIVE),
        new Injury(14L, 70L, 2025, 6, "Concussion", "Concussion", 1, RECOVERED),
        new Injury(15L, 70L, 2026, 7, "Hamstring", "Strain", 1, ACTIVE)
    );

    private final Map<Long, List<Injury>> byPlayer = INJURIES.stream()
        .collect(Collectors.groupingBy(Injury::playerId));

    public InMemoryInjuryRepository() {
        super(INJURIES, Injury::id);
    }

    @Override
    public List<Injury> findByPlayerId(Long playerId) {
        return byPlayer.getOrDefault(playerId, List.of());
    }

    @Override
    public boolean hasActiveInjury(Long playerId) {
        return findByPlayerId(playerId).stream()
            .anyMatch(injury -> injury.status() == InjuryStatus.ACTIVE);
    }
}
