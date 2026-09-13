package com.sidelined.backend.service;

import com.sidelined.backend.model.Team;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
public class TeamService {

    public List<Team> getAllTeams() {
        return Arrays.asList(
            new Team("ARI", "Arizona Cardinals"),
            new Team("ATL", "Atlanta Falcons"),
            new Team("BAL", "Baltimore Ravens"),
            new Team("BUF", "Buffalo Bills"),
            new Team("CAR", "Carolina Panthers"),
            new Team("CHI", "Chicago Bears"),
            new Team("CIN", "Cincinnati Bengals"),
            new Team("CLE", "Cleveland Browns"),
            new Team("DAL", "Dallas Cowboys"),
            new Team("DEN", "Denver Broncos"),
            new Team("DET", "Detroit Lions"),
            new Team("GB",  "Green Bay Packers"),
            new Team("HOU", "Houston Texans"),
            new Team("IND", "Indianapolis Colts"),
            new Team("JAX", "Jacksonville Jaguars"),
            new Team("KC",  "Kansas City Chiefs"),
            new Team("LAC", "Los Angeles Chargers"),
            new Team("LAR", "Los Angeles Rams"),
            new Team("LV",  "Las Vegas Raiders"),
            new Team("MIA", "Miami Dolphins"),
            new Team("MIN", "Minnesota Vikings"),
            new Team("NE",  "New England Patriots"),
            new Team("NO",  "New Orleans Saints"),
            new Team("NYG", "New York Giants"),
            new Team("NYJ", "New York Jets"),
            new Team("PHI", "Philadelphia Eagles"),
            new Team("PIT", "Pittsburgh Steelers"),
            new Team("SEA", "Seattle Seahawks"),
            new Team("SF",  "San Francisco 49ers"),
            new Team("TB",  "Tampa Bay Buccaneers"),
            new Team("TEN", "Tennessee Titans"),
            new Team("WAS", "Washington Commanders")
        );
    }
}