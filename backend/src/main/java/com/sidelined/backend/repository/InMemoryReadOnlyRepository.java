package com.sidelined.backend.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

// Shared lookups for read-only data held in memory. Subclasses supply the data
// and say which field is the id; the lookup code lives here once.
public abstract class InMemoryReadOnlyRepository<T, ID>
        implements ReadOnlyRepository<T, ID> {

    private final List<T> items;
    // Lookups by id without scanning the whole list.
    // Collectors.toMap also fails fast on a duplicate.
    private final Map<ID, T> byId;

    protected InMemoryReadOnlyRepository(List<T> items, Function<T, ID> idOf) {
        this.items = items;
        this.byId = items.stream()
            .collect(Collectors.toMap(idOf, Function.identity()));
    }

    // In the order the subclass listed them.
    @Override
    public List<T> findAll() {
        return items;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public boolean existsById(ID id) {
        return byId.containsKey(id);
    }
}
