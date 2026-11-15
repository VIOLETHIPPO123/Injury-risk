package com.sidelined.backend.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// Stores a slot's positions as one comma-separated column: [RB, WR, TE] <-> "RB,WR,TE".
@Converter
public class RosterPositionsConverter implements AttributeConverter<List<RosterPosition>, String> {

    @Override
    public String convertToDatabaseColumn(List<RosterPosition> positions) {
        return positions.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public List<RosterPosition> convertToEntityAttribute(String column) {
        if (column == null || column.isBlank()) {
            return List.of();
        }
        return Arrays.stream(column.split(",")).map(String::trim).map(RosterPosition::valueOf).toList();
    }
}
