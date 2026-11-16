package com.sidelined.backend.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import java.util.List;
import java.util.Optional;

// A league's roster rules. maxRosterSize counts starters and bench; IR spots are on top of it.
// A read-only reference table: RosterDataLoader inserts StandardRosterFormats at startup and
// nothing writes to it after that. Each format's slots are rows in roster_format_slots.
// Jackson reads the fields, in order, so the JSON is what it was when this was a record.
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Entity
@Immutable
@Table(name = "roster_formats")
public class RosterFormat {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false)
    private String name;

    // Every rule reads the slots, and there are only a handful, so they load with the format.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "roster_format_slots", joinColumns = @JoinColumn(name = "format_id"))
    @OrderColumn(name = "slot_order")
    private List<RosterSlot> slots;

    @Column(nullable = false)
    private int benchCount;

    @Column(nullable = false)
    private int irCount;

    @Column(nullable = false)
    private int maxRosterSize;

    // Where the format appears in GET /roster/formats. Not part of the JSON.
    @JsonIgnore
    @Column(nullable = false)
    private int displayOrder;

    // For JPA.
    protected RosterFormat() {
    }

    public RosterFormat(String id, String name, List<RosterSlot> slots,
                        int benchCount, int irCount, int maxRosterSize, int displayOrder) {
        this.id = id;
        this.name = name;
        this.slots = slots;
        this.benchCount = benchCount;
        this.irCount = irCount;
        this.maxRosterSize = maxRosterSize;
        this.displayOrder = displayOrder;
    }

    public String id()            { return id; }
    public String name()          { return name; }
    public List<RosterSlot> slots() { return slots; }
    public int benchCount()       { return benchCount; }
    public int irCount()          { return irCount; }
    public int maxRosterSize()    { return maxRosterSize; }
    public int displayOrder()     { return displayOrder; }

    // The starting slot with this name; empty for BENCH, IR or a name the format doesn't have.
    public Optional<RosterSlot> findSlot(String name) {
        return slots.stream().filter(s -> s.name().equals(name)).findFirst();
    }
}
