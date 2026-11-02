package com.sidelined.backend.repository;

import com.sidelined.backend.model.Injury;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

// Hard-coded stand-in until the external API module. These injuries are SAMPLE DATA, made up
// to exercise the injury history view: the players are real, the injuries are not.
// Keyed by player id, as in InMemoryPlayerRepository.
@Repository
public class InMemoryInjuryRepository implements InjuryRepository {

    private static final Map<Long, List<Injury>> INJURIES = Map.of(
        // Christian McCaffrey: a repeated hamstring injury.
        321L, List.of(
            new Injury(2023, 5, "Hamstring", "Strain", 1, "Recovered"),
            new Injury(2024, 1, "Achilles", "Tendinitis", 8, "Recovered"),
            new Injury(2025, 9, "Hamstring", "Strain", 2, "Recovered"),
            new Injury(2026, 6, "Calf", "Strain", 1, "Questionable")),
        // Bijan Robinson: a repeated knee injury.
        13L, List.of(
            new Injury(2025, 14, "Knee", "Sprain", 1, "Recovered"),
            new Injury(2026, 7, "Knee", "Bone bruise", 0, "Questionable")),
        // Ja'Marr Chase
        69L, List.of(
            new Injury(2024, 12, "Hip", "Contusion", 0, "Recovered"),
            new Injury(2025, 3, "Ankle", "Sprain", 2, "Recovered")),
        // CeeDee Lamb
        92L, List.of(
            new Injury(2026, 4, "Ankle", "Sprain", 1, "Questionable")),
        // Travis Kelce
        173L, List.of(
            new Injury(2023, 1, "Knee", "Hyperextension", 1, "Recovered")),
        // Javonte Williams
        91L, List.of(
            new Injury(2022, 4, "Knee", "Torn ACL", 13, "Recovered"),
            new Injury(2025, 10, "Hamstring", "Strain", 1, "Recovered")),
        // Mark Andrews and Tee Higgins: hurt now ("Active"), so they can go on IR.
        28L, List.of(
            new Injury(2026, 8, "Ankle", "High sprain", 0, "Active")),
        70L, List.of(
            new Injury(2025, 6, "Concussion", "Concussion", 1, "Recovered"),
            new Injury(2026, 7, "Hamstring", "Strain", 1, "Active"))
    );

    @Override
    public List<Injury> findByPlayerId(Long playerId) {
        return INJURIES.getOrDefault(playerId, List.of());
    }
}
