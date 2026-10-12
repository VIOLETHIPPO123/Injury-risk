package com.sidelined.backend.validation;

import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.model.WatchlistEntry;
import com.sidelined.backend.service.PlayerLookup;
import org.springframework.stereotype.Component;

// Rule: you can only watch a player that exists.
@Component
public class PlayerExistsValidator implements WatchlistEntryValidator {

    private final PlayerLookup playerLookup;

    public PlayerExistsValidator(PlayerLookup playerLookup) {
        this.playerLookup = playerLookup;
    }

    @Override
    public void validate(WatchlistEntry request) {
        if (!playerLookup.existsById(request.getPlayerId())) {
            throw new PlayerNotFoundException(request.getPlayerId());
        }
    }
}
