package com.sidelined.backend.service;

import com.sidelined.backend.exception.NflverseUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

// The only code that downloads from nflverse (CSV files attached to its GitHub releases).
// It hands back the rows as nflverse writes them; the sync jobs decide what they mean.
@Service
public class NflverseClient {

    private final RestClient restClient;
    private final Clock clock;

    @Autowired
    public NflverseClient(@Value("${nflverse.base-url}") String baseUrl) {
        this(RestClient.builder().requestFactory(followingRedirects()), baseUrl, Clock.systemUTC());
    }

    // Tests pass a builder bound to a mock server, and a fixed clock.
    NflverseClient(RestClient.Builder builder, String baseUrl, Clock clock) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.clock = clock;
    }

    // GitHub answers a release download with a redirect to its file store, which Java's HTTP
    // client doesn't follow unless told to.
    private static JdkClientHttpRequestFactory followingRedirects() {
        HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(60));
        return factory;
    }

    // Everyone nflverse has a player id for, past and present (about 25,000 rows).
    public List<NflversePlayer> players() {
        String path = "/players/players.csv";
        return download(path, csv -> {
            List<NflversePlayer> players = new ArrayList<>();
            for (CsvReader.Row row = csv.next(); row != null; row = csv.next()) {
                players.add(new NflversePlayer(row.get("gsis_id"), row.get("pfr_id"),
                    row.get("display_name"), row.get("position")));
            }
            return players;
        }).orElseThrow(() -> new NflverseUnavailableException("nflverse has no " + path));
    }

    // Every team's depth chart as of nflverse's newest snapshot.
    public List<DepthChartEntry> latestDepthChart() {
        return currentSeason("depth chart", this::depthChart);
    }

    // Every player's snaps in every game of the current season so far, playoffs included.
    public List<SnapCount> snapCounts() {
        return currentSeason("snap counts", this::snapCounts);
    }

    // nflverse keeps one file per season, named for the year the season started, and next
    // season's doesn't exist until that season is near (March for depth charts, September for
    // snap counts). So a missing file for this year means last year's is still the current one.
    private <T> T currentSeason(String what, IntFunction<Optional<T>> fileForSeason) {
        int year = Year.now(clock).getValue();
        return fileForSeason.apply(year)
            .or(() -> fileForSeason.apply(year - 1))
            .orElseThrow(() -> new NflverseUnavailableException(
                "nflverse has no " + what + " for " + year + " or " + (year - 1)));
    }

    private Optional<List<SnapCount>> snapCounts(int season) {
        return download("/snap_counts/snap_counts_" + season + ".csv", csv -> {
            List<SnapCount> snapCounts = new ArrayList<>();
            for (CsvReader.Row row = csv.next(); row != null; row = csv.next()) {
                int snaps = count(row.get("offense_snaps")) + count(row.get("defense_snaps"))
                    + count(row.get("st_snaps"));
                snapCounts.add(new SnapCount(row.get("pfr_player_id"), row.get("player"),
                    count(row.get("season")), count(row.get("week")), snaps));
            }
            return snapCounts;
        });
    }

    // A whole number, which nflverse sometimes writes with a decimal point ("55.0"). Blank is 0.
    // Anything else isn't a count, and fails the download rather than being guessed at.
    private static int count(String text) {
        return text.isBlank() ? 0 : (int) Double.parseDouble(text.trim());
    }

    // The file holds a snapshot of all 32 teams for every day of the season, newest first,
    // each stamped with the time it was taken ("dt"). Only the first snapshot is read: the
    // download stops at the first row with an older stamp, a few thousand rows into a file
    // of hundreds of thousands.
    private Optional<List<DepthChartEntry>> depthChart(int season) {
        return download("/depth_charts/depth_charts_" + season + ".csv", csv -> {
            List<DepthChartEntry> entries = new ArrayList<>();
            String newest = null;
            for (CsvReader.Row row = csv.next(); row != null; row = csv.next()) {
                String takenAt = row.get("dt");
                if (newest == null) {
                    newest = takenAt;
                } else if (!newest.equals(takenAt)) {
                    break;
                }
                entries.add(new DepthChartEntry(row.get("gsis_id"), row.get("team"),
                    row.get("pos_abb"), rank(row.get("pos_rank"))));
            }
            return entries;
        });
    }

    // A rank that isn't a number sorts behind every real one.
    private static int rank(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private interface CsvParser<T> {
        T parse(CsvReader csv) throws IOException;
    }

    // Empty if nflverse has no such file (404). Any other failure is an exception.
    private <T> Optional<T> download(String path, CsvParser<T> parser) {
        try {
            return restClient.get().uri(path).exchange((request, response) -> {
                if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                    return Optional.<T>empty();
                }
                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new NflverseUnavailableException(
                        "nflverse answered " + response.getStatusCode() + " for " + path);
                }
                return Optional.of(parser.parse(new CsvReader(
                    new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))));
            });
        } catch (RestClientException e) {
            throw new NflverseUnavailableException("Couldn't download " + path + " from nflverse", e);
        } catch (IllegalStateException | NumberFormatException e) {
            // A column we read is gone or isn't a number any more: nflverse changed the file's format.
            throw new NflverseUnavailableException(path + " isn't in the expected format: " + e.getMessage(), e);
        }
    }
}
