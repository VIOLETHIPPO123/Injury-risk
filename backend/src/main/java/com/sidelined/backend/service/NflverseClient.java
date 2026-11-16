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

// The only code that downloads from nflverse (CSV files attached to its GitHub releases).
// It hands back the rows as nflverse writes them; PlayerSyncJob decides what they mean.
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
                players.add(new NflversePlayer(
                    row.get("gsis_id"), row.get("display_name"), row.get("position")));
            }
            return players;
        }).orElseThrow(() -> new NflverseUnavailableException("nflverse has no " + path));
    }

    // Every team's depth chart as of nflverse's newest snapshot. There is one file per season,
    // and next season's file doesn't exist until around March, so a missing file for this
    // year means last year's is still the current one.
    public List<DepthChartEntry> latestDepthChart() {
        int year = Year.now(clock).getValue();
        return depthChart(year)
            .or(() -> depthChart(year - 1))
            .orElseThrow(() -> new NflverseUnavailableException(
                "nflverse has no depth chart for " + year + " or " + (year - 1)));
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
        } catch (IllegalStateException e) {
            // A column we read is gone: nflverse changed the file's format.
            throw new NflverseUnavailableException(path + " isn't in the expected format: " + e.getMessage(), e);
        }
    }
}
