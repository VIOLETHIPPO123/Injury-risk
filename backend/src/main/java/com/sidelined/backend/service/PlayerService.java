package com.sidelined.backend.service;

import com.sidelined.backend.model.Player;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
public class PlayerService {

    public List<Player> getAllPlayers() {
        return Arrays.asList(
            new Player(1L, "Christian McCaffrey", "SF", 58, 212),
            new Player(2L, "Saquon Barkley", "PHI", 47, 168),
            new Player(3L, "Ja'Marr Chase", "CIN", 62, 240),
            new Player(4L, "Nick Chubb", "CLE", 31, 96)
        );
    }
}
