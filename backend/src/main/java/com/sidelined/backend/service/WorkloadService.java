package com.sidelined.backend.service;

import com.sidelined.backend.exception.PlayerNotFoundException;
import com.sidelined.backend.model.WorkloadEntry;
import com.sidelined.backend.repository.PlayerRepository;
import com.sidelined.backend.repository.WorkloadRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class WorkloadService {

    private final WorkloadRepository workloadRepository;
    private final PlayerRepository playerRepository;

    public WorkloadService(WorkloadRepository workloadRepository, PlayerRepository playerRepository) {
        this.workloadRepository = workloadRepository;
        this.playerRepository = playerRepository;
    }

    // An unknown player is a 404 (via ApiExceptionHandler).
    public List<WorkloadEntry> getWorkload(Long playerId) {
        if (!playerRepository.existsById(playerId)) {
            throw new PlayerNotFoundException(playerId);
        }
        return workloadRepository.findByPlayerId(playerId);
    }
}
