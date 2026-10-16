package com.sidelined.backend.validation;

import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.model.WatchlistEntry;
import com.sidelined.backend.repository.PlayerRepository;
import org.springframework.stereotype.Component;

// Rule: you can only watch a player that exists.
@Component
public class PlayerExistsValidator implements WatchlistEntryValidator {

    private final PlayerRepository playerRepository;

    public PlayerExistsValidator(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public void validate(WatchlistEntry request) {
        if (!playerRepository.existsById(request.getPlayerId())) {
            throw new PlayerNotFoundException(request.getPlayerId());
        }
    }
}
