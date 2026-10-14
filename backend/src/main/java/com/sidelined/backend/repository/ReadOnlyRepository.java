package com.sidelined.backend.repository;

import java.util.List;
import java.util.Optional;

// What every repository can do: look things up. Where the data comes from (a hard-coded
// list today, an external API or a database later) is the implementation's business.
// Signatures match Spring Data so a JPA repository can implement these directly.
public interface ReadOnlyRepository<T, ID> {
    List<T> findAll();
    Optional<T> findById(ID id);
    boolean existsById(ID id);
}
