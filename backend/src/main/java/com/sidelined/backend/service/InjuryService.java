package com.sidelined.backend.service;

import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.model.Injury;
import com.sidelined.backend.repository.InjuryRepository;
import com.sidelined.backend.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;

@Service
public class InjuryService {

    // Newest first: the latest season, then the latest week within it.
    private static final Comparator<Injury> NEWEST_FIRST = Comparator
        .comparingInt(Injury::season).thenComparingInt(Injury::week).reversed();

    private final InjuryRepository injuryRepository;
    private final PlayerRepository playerRepository;

    public InjuryService(InjuryRepository injuryRepository, PlayerRepository playerRepository) {
        this.injuryRepository = injuryRepository;
        this.playerRepository = playerRepository;
    }

    // An unknown player is a 404 (via ApiExceptionHandler); a known player with no injuries
    // is an empty list.
    public List<Injury> getInjuries(Long playerId) {
        if (!playerRepository.existsById(playerId)) {
            throw new PlayerNotFoundException(playerId);
        }
        return injuryRepository.findByPlayerId(playerId).stream().sorted(NEWEST_FIRST).toList();
    }
}
