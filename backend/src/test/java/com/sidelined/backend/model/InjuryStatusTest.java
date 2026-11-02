package com.sidelined.backend.model;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InjuryStatusTest {

    @Test
    void theOnlyStatuses_areActiveRecoveringAndRecovered() {
        List<String> labels = Arrays.stream(InjuryStatus.values()).map(InjuryStatus::getLabel).toList();

        assertEquals(List.of("Active", "Recovering", "Recovered"), labels);
    }
}
