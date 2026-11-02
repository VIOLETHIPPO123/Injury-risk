package com.sidelined.backend.validation;

import com.sidelined.backend.exception.NoActiveInjuryException;
import com.sidelined.backend.model.Injury;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.InjuryRepository;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InjuredReserveValidatorTest {

    private final InjuryRepository injuryRepository = mock(InjuryRepository.class);
    private final InjuredReserveValidator validator = new InjuredReserveValidator(injuryRepository);

    private static final RosterEntry ENTRY = new RosterEntry(1L, 28L, RosterEntry.BENCH, "");

    private static Injury injury(String status) {
        return new Injury(2026, 8, "Ankle", "Sprain", 0, status);
    }

    @Test
    void validate_toIr_withAnActiveInjury_passes() {
        when(injuryRepository.findByPlayerId(28L)).thenReturn(List.of(injury("Recovered"), injury("Active")));

        assertDoesNotThrow(() -> validator.validate(ENTRY, RosterEntry.IR));
    }

    @Test
    void validate_toIr_withOnlyPastOrQuestionableInjuries_throws() {
        when(injuryRepository.findByPlayerId(28L)).thenReturn(List.of(injury("Recovered"), injury("Questionable")));

        assertThrows(NoActiveInjuryException.class, () -> validator.validate(ENTRY, RosterEntry.IR));
    }

    @Test
    void validate_toIr_withNoInjuries_throws() {
        when(injuryRepository.findByPlayerId(28L)).thenReturn(List.of());

        assertThrows(NoActiveInjuryException.class, () -> validator.validate(ENTRY, RosterEntry.IR));
    }

    @Test
    void validate_anyOtherSlot_doesntCheckInjuries() {
        assertDoesNotThrow(() -> validator.validate(ENTRY, "TE"));
        assertDoesNotThrow(() -> validator.validate(ENTRY, RosterEntry.BENCH));
        verifyNoInteractions(injuryRepository);
    }
}
