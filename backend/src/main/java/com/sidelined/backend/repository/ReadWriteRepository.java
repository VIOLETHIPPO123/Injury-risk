package com.sidelined.backend.repository;

// A repository that can also be written to. Read-only sources (players and teams, which
// will come from an external API) never extend this, so they never have to fake writes.
public interface ReadWriteRepository<T, ID> extends ReadOnlyRepository<T, ID> {
    T save(T entity);
    void deleteById(ID id);
}
