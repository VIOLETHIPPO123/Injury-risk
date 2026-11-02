package com.sidelined.backend.validation;

import com.sidelined.backend.exception.NoActiveInjuryException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.InjuryRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Rule: only a player with an Active injury can go on IR.
@Component
@Order(3)
public class InjuredReserveValidator implements SlotMoveValidator {

    private static final String ACTIVE = "Active";

    private final InjuryRepository injuryRepository;

    public InjuredReserveValidator(InjuryRepository injuryRepository) {
        this.injuryRepository = injuryRepository;
    }

    @Override
    public void validate(RosterEntry entry, String slot) {
        if (!RosterEntry.IR.equals(slot)) {
            return;
        }
        boolean activelyInjured = injuryRepository.findByPlayerId(entry.getPlayerId()).stream()
            .anyMatch(injury -> ACTIVE.equals(injury.status()));
        if (!activelyInjured) {
            throw new NoActiveInjuryException(entry.getPlayerId());
        }
    }
}
