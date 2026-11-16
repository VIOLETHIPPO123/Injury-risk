package com.sidelined.backend.validation;

import com.sidelined.backend.exception.NoActiveInjuryException;
import com.sidelined.backend.model.RosterEntry;
import com.sidelined.backend.repository.InjuryRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Which injuries count as Active is tested in InMemoryInjuryRepositoryTest.
class InjuredReserveValidatorTest {

    // The signed-in user whose roster is being checked.
    private static final String USER = "user-a";

    private final InjuryRepository injuryRepository = mock(InjuryRepository.class);
    private final InjuredReserveValidator validator = new InjuredReserveValidator(injuryRepository);

    private static final RosterEntry ENTRY = new RosterEntry(1L, 28L, RosterEntry.BENCH, "");

    @Test
    void validate_toIr_withAnActiveInjury_passes() {
        when(injuryRepository.hasActiveInjury(28L)).thenReturn(true);

        assertDoesNotThrow(() -> validator.validate(USER, ENTRY, RosterEntry.IR));
    }

    @Test
    void validate_toIr_withoutAnActiveInjury_throws() {
        when(injuryRepository.hasActiveInjury(28L)).thenReturn(false);

        assertThrows(NoActiveInjuryException.class, () -> validator.validate(USER, ENTRY, RosterEntry.IR));
    }

    @Test
    void validate_anyOtherSlot_doesntCheckInjuries() {
        assertDoesNotThrow(() -> validator.validate(USER, ENTRY, "TE"));
        assertDoesNotThrow(() -> validator.validate(USER, ENTRY, RosterEntry.BENCH));
        verifyNoInteractions(injuryRepository);
    }
}
