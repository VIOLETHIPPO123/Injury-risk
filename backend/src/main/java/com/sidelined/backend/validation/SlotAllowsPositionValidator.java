package com.sidelined.backend.validation;

import com.sidelined.backend.exception.SlotNotAllowedException;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.model.RosterFormat;
import com.sidelined.backend.model.RosterPosition;
import com.sidelined.backend.model.RosterSlot;
import com.sidelined.backend.repository.PlayerRepository;
import com.sidelined.backend.service.CurrentRosterFormat;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.Optional;

// Rule: a starting slot only takes the positions it lists (an FB counts as an RB, a PK as a K).
// BENCH and IR take anyone. A slot the format doesn't have takes no one.
@Component
@Order(1)
public class SlotAllowsPositionValidator implements SlotMoveValidator {

    private final PlayerRepository playerRepository;
    private final CurrentRosterFormat currentFormat;

    public SlotAllowsPositionValidator(PlayerRepository playerRepository,
                                       CurrentRosterFormat currentFormat) {
        this.playerRepository = playerRepository;
        this.currentFormat = currentFormat;
    }

    @Override
    public void validate(RosterEntry entry, String slot) {
        if (RosterEntry.BENCH.equals(slot) || RosterEntry.IR.equals(slot)) {
            return;
        }
        RosterFormat format = currentFormat.get();
        RosterSlot target = format.findSlot(slot).orElseThrow(
            () -> new SlotNotAllowedException(format.name() + " has no " + slot + " slot"));

        Player player = playerRepository.findById(entry.getPlayerId()).orElseThrow();
        Optional<RosterPosition> position = RosterPosition.of(player.getPosition());
        if (position.isEmpty() || !target.positions().contains(position.get())) {
            throw new SlotNotAllowedException(
                "A " + player.getPosition() + " can't play " + slot);
        }
    }
}
