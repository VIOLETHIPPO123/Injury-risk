package com.sidelined.backend.validation;

import com.sidelined.backend.exception.NoActiveInjuryException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.InjuryRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Which injuries count as Active is tested in InMemoryInjuryRepositoryTest.
class InjuredReserveValidatorTest {

    private final InjuryRepository injuryRepository = mock(InjuryRepository.class);
    private final InjuredReserveValidator validator = new InjuredReserveValidator(injuryRepository);

    private static final RosterEntry ENTRY = new RosterEntry(1L, 28L, RosterEntry.BENCH, "");

    @Test
    void validate_toIr_withAnActiveInjury_passes() {
        when(injuryRepository.hasActiveInjury(28L)).thenReturn(true);

        assertDoesNotThrow(() -> validator.validate(ENTRY, RosterEntry.IR));
    }

    @Test
    void validate_toIr_withoutAnActiveInjury_throws() {
        when(injuryRepository.hasActiveInjury(28L)).thenReturn(false);

        assertThrows(NoActiveInjuryException.class, () -> validator.validate(ENTRY, RosterEntry.IR));
    }

    @Test
    void validate_anyOtherSlot_doesntCheckInjuries() {
        assertDoesNotThrow(() -> validator.validate(ENTRY, "TE"));
        assertDoesNotThrow(() -> validator.validate(ENTRY, RosterEntry.BENCH));
        verifyNoInteractions(injuryRepository);
    }
}
