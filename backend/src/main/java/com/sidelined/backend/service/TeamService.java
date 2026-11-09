package com.sidelined.backend.service;

import com.sidelined.backend.model.Team;
import com.sidelined.backend.repository.TeamRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    // Alphabetical by code, the same order as before the database. The database has no order of its own.
    public List<Team> getAllTeams() {
        return teamRepository.findAll(Sort.by("code"));
    }
}
