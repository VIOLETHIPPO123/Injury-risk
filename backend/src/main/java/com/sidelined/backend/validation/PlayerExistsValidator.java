package com.sidelined.backend.validation;

import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.PlayerRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Rule: you can only roster a player that exists. Runs first, so the rules after it
// can look the player up.
@Component
@Order(1)
public class PlayerExistsValidator implements RosterEntryValidator {

    private final PlayerRepository playerRepository;

    public PlayerExistsValidator(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public void validate(String userId, RosterEntry request) {
        // A body with no playerId is a missing player too (JPA won't look up a null id).
        if (request.getPlayerId() == null || !playerRepository.existsById(request.getPlayerId())) {
            throw new PlayerNotFoundException(request.getPlayerId());
        }
    }
}
