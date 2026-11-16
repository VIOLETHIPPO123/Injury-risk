package com.sidelined.backend.service;

import com.sidelined.backend.exception.NflverseUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

// nflverse is replaced by a mock server, so nothing is downloaded. The CSV is in the shape of
// nflverse's real files, with only the columns that matter filled in.
class NflverseClientTest {

    private static final String BASE = "https://nflverse.example/download";
    private static final String PLAYERS_URL = BASE + "/players/players.csv";
    private static final String CHART_2026_URL = BASE + "/depth_charts/depth_charts_2026.csv";
    private static final String CHART_2025_URL = BASE + "/depth_charts/depth_charts_2025.csv";
    private static final MediaType CSV = MediaType.parseMediaType("text/csv");

    private static final String CHART_HEADER =
        "dt,team,player_name,espn_id,gsis_id,pos_grp_id,pos_grp,pos_id,pos_name,pos_abb,pos_slot,pos_rank\n";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer nflverse = MockRestServiceServer.bindTo(builder).build();
    private final Clock november2026 = Clock.fixed(Instant.parse("2026-11-17T20:00:00Z"), ZoneOffset.UTC);
    private final NflverseClient client = new NflverseClient(builder, BASE, november2026);

    // ---------- Players ----------

    @Test
    void players_readsIdNameAndPosition_pastQuotedColumnsWithCommas() {
        // Arrange
        String csv = "gsis_id,display_name,first_name,position_group,position,headshot,latest_team,status\n"
            + "00-0038389,Israel Abanikanda,Israel,RB,RB,\"https://static.www.nfl.com/image/upload/f_auto,q_auto/league/x\",DAL,ACT\n"
            + "00-0033873,Patrick Mahomes,Patrick,QB,QB,,KC,ACT\n";
        nflverse.expect(requestTo(PLAYERS_URL)).andRespond(withSuccess(csv, CSV));

        // Act
        List<NflversePlayer> players = client.players();

        // Assert
        nflverse.verify();
        assertEquals(List.of(
            new NflversePlayer("00-0038389", "Israel Abanikanda", "RB"),
            new NflversePlayer("00-0033873", "Patrick Mahomes", "QB")), players);
    }

    @Test
    void players_fileMissing_throws() {
        nflverse.expect(requestTo(PLAYERS_URL)).andRespond(withResourceNotFound());

        assertThrows(NflverseUnavailableException.class, client::players);
    }

    @Test
    void players_serverError_throws() {
        nflverse.expect(requestTo(PLAYERS_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(NflverseUnavailableException.class, client::players);
    }

    @Test
    void players_nflverseUnreachable_throws() {
        nflverse.expect(requestTo(PLAYERS_URL)).andRespond(withException(new IOException("connection reset")));

        assertThrows(NflverseUnavailableException.class, client::players);
    }

    @Test
    void players_fileWithoutAColumnWeRead_throws() {
        // nflverse renamed display_name.
        nflverse.expect(requestTo(PLAYERS_URL))
            .andRespond(withSuccess("gsis_id,full_name,position\n00-0033873,Patrick Mahomes,QB\n", CSV));

        assertThrows(NflverseUnavailableException.class, client::players);
    }

    // ---------- Depth chart ----------

    @Test
    void latestDepthChart_readsOnlyTheNewestSnapshot() {
        // Arrange: the file is newest first; the second snapshot is yesterday's.
        String csv = CHART_HEADER
            + "2026-11-15T14:41:31Z,ARI,Josh Sweat,3693166,00-0034381,16,Base 4-3 D,11,Left Defensive End,LDE,1,1\n"
            + "2026-11-15T14:41:31Z,LA,Matthew Stafford,12483,00-0026498,21,3WR 1TE,8,Quarterback,QB,5,1\n"
            + "2026-11-15T14:41:31Z,LA,Somebody Else,1,00-0030000,21,3WR 1TE,8,Quarterback,QB,5,2\n"
            + "2026-11-14T14:04:32Z,ARI,Josh Sweat,3693166,00-0034381,16,Base 4-3 D,11,Left Defensive End,LDE,1,1\n"
            + "2026-11-14T14:04:32Z,ARI,Cut Yesterday,2,00-0031111,16,Base 4-3 D,11,Left Defensive End,LDE,1,2\n";
        nflverse.expect(requestTo(CHART_2026_URL)).andRespond(withSuccess(csv, CSV));

        // Act
        List<DepthChartEntry> chart = client.latestDepthChart();

        // Assert
        nflverse.verify();
        assertEquals(List.of(
            new DepthChartEntry("00-0034381", "ARI", "LDE", 1),
            new DepthChartEntry("00-0026498", "LA", "QB", 1),
            new DepthChartEntry("00-0030000", "LA", "QB", 2)), chart);
    }

    @Test
    void latestDepthChart_noFileForThisYearYet_usesLastYears() {
        // Arrange: in January and February the season that started last year is still current.
        Clock january2027 = Clock.fixed(Instant.parse("2027-01-12T20:00:00Z"), ZoneOffset.UTC);
        NflverseClient januaryClient = new NflverseClient(builder, BASE, january2027);
        nflverse.expect(requestTo(BASE + "/depth_charts/depth_charts_2027.csv")).andRespond(withResourceNotFound());
        nflverse.expect(requestTo(CHART_2026_URL)).andRespond(withSuccess(CHART_HEADER
            + "2027-01-11T14:00:00Z,KC,Patrick Mahomes,3139477,00-0033873,21,3WR 1TE,8,Quarterback,QB,5,1\n", CSV));

        // Act
        List<DepthChartEntry> chart = januaryClient.latestDepthChart();

        // Assert
        nflverse.verify();
        assertEquals(List.of(new DepthChartEntry("00-0033873", "KC", "QB", 1)), chart);
    }

    @Test
    void latestDepthChart_noFileForEitherYear_throws() {
        nflverse.expect(requestTo(CHART_2026_URL)).andRespond(withResourceNotFound());
        nflverse.expect(requestTo(CHART_2025_URL)).andRespond(withResourceNotFound());

        assertThrows(NflverseUnavailableException.class, client::latestDepthChart);
    }

    @Test
    void latestDepthChart_rankThatIsntANumber_sortsLast() {
        nflverse.expect(requestTo(CHART_2026_URL)).andRespond(withSuccess(CHART_HEADER
            + "2026-11-15T14:41:31Z,KC,Patrick Mahomes,3139477,00-0033873,21,3WR 1TE,8,Quarterback,QB,5,NA\n", CSV));

        assertEquals(Integer.MAX_VALUE, client.latestDepthChart().get(0).rank());
    }
}
