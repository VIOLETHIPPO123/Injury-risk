package com.sidelined.backend.repository;

import com.sidelined.backend.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// The players table. Spring Data writes the implementation. The table stays empty until
// DEV-102's sync job fills it; until then, add test rows in the Supabase Table Editor.
public interface JpaPlayerRepository extends JpaRepository<Player, Long>, PlayerRepository {

    // For the sync job (DEV-102), which updates players by their nflverse id.
    Optional<Player> findByGsisId(String gsisId);
}
