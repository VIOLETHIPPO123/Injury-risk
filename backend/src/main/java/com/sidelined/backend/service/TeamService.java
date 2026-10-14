package com.sidelined.backend.service;

import com.sidelined.backend.model.Team;
import com.sidelined.backend.repository.TeamRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }
}
