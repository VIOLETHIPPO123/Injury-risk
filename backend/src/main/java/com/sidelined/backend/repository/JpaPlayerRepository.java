package com.sidelined.backend.repository;

import com.sidelined.backend.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

// The players table. Spring Data writes the implementation. PlayerSyncJob fills it from nflverse.
public interface JpaPlayerRepository extends JpaRepository<Player, Long>, PlayerRepository {

    // A player by their nflverse id.
    Optional<Player> findByGsisId(String gsisId);

    // Every player at a position, by its name ("DEF"): the column is plain text (see Player).
    List<Player> findByPosition(String position);
}
