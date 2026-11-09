package com.sidelined.backend;

import com.sidelined.backend.config.TeamDataLoader;
import com.sidelined.backend.model.NflTeam;
import com.sidelined.backend.model.Player;
import com.sidelined.backend.model.Position;
import com.sidelined.backend.model.Workload;
import com.sidelined.backend.repository.JpaPlayerRepository;
import com.sidelined.backend.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

// Starts the whole app against an in-memory H2 database (the "test" profile), so the JPA
// mappings and the team loading are checked against real tables without touching Supabase.
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private TeamDataLoader teamDataLoader;

	@Autowired
	private JpaPlayerRepository playerRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void teamsTable_has32Teams_andLoadingAgainAddsNone() {
		assertEquals(32, teamRepository.count());

		// A restart runs the loader again.
		teamDataLoader.run(null);

		assertEquals(32, teamRepository.count());
		assertEquals("Kansas City Chiefs", teamRepository.findById(NflTeam.KC).orElseThrow().getName());
	}

	@Test
	void playersTable_savesAPlayer_findableByGsisId_withGsisIdHiddenFromJson() {
		// Arrange
		Player saved = playerRepository.save(
			new Player(null, "00-0034796", "Lamar Jackson", NflTeam.BAL, Position.QB, new Workload(63, 261)));
		// Team defenses have no gsis_id, so many rows must be able to leave it empty.
		playerRepository.save(new Player(null, null, "Test One", NflTeam.ARI, Position.WR, new Workload(1, 4)));
		playerRepository.save(new Player(null, null, "Test Two", NflTeam.ARI, Position.WR, new Workload(1, 4)));

		// Act
		Player found = playerRepository.findByGsisId("00-0034796").orElseThrow();

		// Assert
		assertNotNull(saved.getId());
		assertEquals(saved.getId(), found.getId());
		assertEquals(NflTeam.BAL, found.getTeam());
		assertEquals(Position.QB, found.getPosition());
		assertEquals(63, found.getSnapsLastGame());
		assertEquals(261, found.getSnapsLast4Games());

		String json = JsonMapper.builder().build().writeValueAsString(found);
		assertFalse(json.contains("gsis"), json);
		assertEquals("{\"id\":" + found.getId() + ",\"name\":\"Lamar Jackson\",\"team\":\"BAL\",\"position\":\"QB\","
			+ "\"snapsLastGame\":63,\"snapsLast4Games\":261}", json);
	}
}
