package com.sidelined.backend.validation;

import com.sidelined.backend.exception.NotFantasyEligibleException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.RosterPosition;
import com.sidelined.backend.repository.PlayerRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Rule: only QBs, RBs (fullbacks included), WRs, TEs and kickers can be rostered.
// Linemen, individual defenders, punters, holders, snappers and return men can't.
@Component
@Order(2)
public class FantasyEligibleValidator implements RosterEntryValidator {

    private final PlayerRepository playerRepository;

    public FantasyEligibleValidator(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public void validate(RosterEntry request) {
        playerRepository.findById(request.getPlayerId()).ifPresent(player -> {
            if (RosterPosition.of(player.getPosition()).isEmpty()) {
                throw new NotFantasyEligibleException();
            }
        });
    }
}
