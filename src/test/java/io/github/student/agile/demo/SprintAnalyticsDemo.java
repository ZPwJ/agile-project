package io.github.student.agile.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class SprintAnalyticsDemo {

    private static final String LOCAL_API_URL = "http://localhost:8081";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiUrl = System.getProperty("demo.api", LOCAL_API_URL);
    private String token;

    @Test
    void createAndVerifyDemo() throws Exception {
        login();
        String run = Instant.now().toString();
        int project = project("Demo raportów " + run);
        int otherProject = project("Demo osobnego projektu " + run);
        System.out.printf("Projekty: główny=%d, osobny=%d%n", project, otherProject);

        int first = sprint(project, "Sprint 1, \"zmiana zakresu\"");
        int done = issue(project, "Ukończone", 3);
        int unestimated = issue(project, "Bez estymacji", null);
        int carried = issue(project, "Przenoszone", 5);
        attach(first, done);
        attach(first, unestimated);
        json("PATCH", "/api/sprint/start/" + first, null);
        attach(first, carried);
        complete(done);
        json("PATCH", "/api/sprint/project/" + project + "/stop", null);
        JsonNode firstReport = json("GET", "/api/reports/sprints/" + first, null);
        assertThat(firstReport.path("initialIssueCount").asInt()).isEqualTo(2);
        verifyResult(firstReport, 1, 2, 3, 5);

        int second = sprint(project, "Sprint 2 — bez estymacji");
        attach(second, carried);
        attach(second, unestimated);
        json("PATCH", "/api/sprint/start/" + second, null);
        reestimate(carried, "Przenoszone", 8);
        complete(unestimated);
        json("PATCH", "/api/sprint/project/" + project + "/stop", null);
        JsonNode secondReport = json("GET", "/api/reports/sprints/" + second, null);
        verifyResult(secondReport, 1, 1, 0, 8);
        reestimate(done, "Ukończone", 99);
        assertThat(json("GET", "/api/reports/sprints/" + first, null)).isEqualTo(firstReport);
        JsonNode carriedDetails = json("GET", "/api/issue/details/" + carried, null);
        assertThat(carriedDetails.path("sprintID").isNull()).isTrue();
        assertThat(carriedDetails.path("issueStatusID").asInt()).isEqualTo(1);

        int third = sprint(otherProject, "Sprint osobnego projektu");
        int otherDone = issue(otherProject, "Osobne zadanie", 13);
        attach(third, otherDone);
        json("PATCH", "/api/sprint/start/" + third, null);
        complete(otherDone);
        json("PATCH", "/api/sprint/project/" + otherProject + "/stop", null);
        verifyResult(json("GET", "/api/reports/sprints/" + third, null), 1, 0, 13, 0);

        verifyCharts(first, firstReport);
        verifyCharts(second, secondReport);
        JsonNode firstUp = json("GET", "/api/reports/sprints/" + first + "/burnup", null);
        assertThat(firstUp.get(0).path("totalPoints").asLong()).isEqualTo(3);
        assertThat(firstUp.get(1).path("totalPoints").asLong()).isEqualTo(8);

        var from = Instant.parse(firstReport.path("endDate").asText()).atOffset(ZoneOffset.UTC).toLocalDate();
        var to = Instant.parse(secondReport.path("endDate").asText()).atOffset(ZoneOffset.UTC).toLocalDate();
        String query = "projectID=" + project + "&from=" + from + "&to=" + to;
        assertThat(json("GET", "/api/reports/sprints?" + query, null))
                .isEqualTo(mapper.valueToTree(List.of(firstReport, secondReport)));
        String header = "projectID,sprintID,name,startDate,endDate,completedIssueCount,incompleteIssueCount,velocity\r\n";
        String csv = send("GET", "/api/reports/sprints/csv?" + query, null);
        assertThat(csv).isEqualTo(header
                + "%d,%d,\"Sprint 1, \"\"zmiana zakresu\"\"\",%s,%s,1,2,3\r\n".formatted(project, first,
                        firstReport.path("startDate").asText(), firstReport.path("endDate").asText())
                + "%d,%d,Sprint 2 — bez estymacji,%s,%s,1,1,0\r\n".formatted(project, second,
                        secondReport.path("startDate").asText(), secondReport.path("endDate").asText()));
        String emptyQuery = "projectID=" + project + "&from=" + to.plusDays(1);
        assertThat(json("GET", "/api/reports/sprints?" + emptyQuery, null).isEmpty()).isTrue();
        assertThat(send("GET", "/api/reports/sprints/csv?" + emptyQuery, null)).isEqualTo(header);
        JsonNode history = json("GET", "/api/issue/product-backlog-history?projectID=" + project, null);
        assertThat(StreamSupport.stream(history.spliterator(), false).map(row -> row.path("id").asInt()).toList())
                .containsExactlyInAnyOrder(done, unestimated);

        System.out.printf("OK: sprinty %d (velocity=3), %d (velocity=0), %d (velocity=13)%n", first, second, third);
        System.out.println("Raport: " + LOCAL_API_URL + "/api/reports/sprints/" + first);
        System.out.println("Burndown: " + LOCAL_API_URL + "/api/reports/sprints/" + first + "/burndown");
        System.out.println("Burnup: " + LOCAL_API_URL + "/api/reports/sprints/" + first + "/burnup");
        System.out.println("Zestawienie: " + LOCAL_API_URL + "/api/reports/sprints?" + query);
        System.out.println("CSV: " + LOCAL_API_URL + "/api/reports/sprints/csv?" + query);
    }

    private void login() throws Exception {
        JsonNode realm = mapper.readTree(Path.of("infrastructure/auth-server/realm-export.json").toFile());
        JsonNode authClient = find(realm.path("clients"), "clientId", "agile-project-server");
        JsonNode user = find(realm.path("users"), "username", "admin@agile.local");
        String form = Map.of("grant_type", "password", "client_id", authClient.path("clientId").asText(),
                        "client_secret", authClient.path("secret").asText(), "username", user.path("username").asText(),
                        "password", find(user.path("credentials"), "type", "password").path("value").asText())
                .entrySet().stream().map(entry -> entry.getKey() + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(
                        "http://localhost:8080/realms/agile-project/protocol/openid-connect/token"))
                .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        token = mapper.readTree(response.body()).path("access_token").asText();
        assertThat(token).isNotBlank();
    }

    private JsonNode find(JsonNode array, String key, String value) {
        return StreamSupport.stream(array.spliterator(), false)
                .filter(item -> value.equals(item.path(key).asText())).findFirst().orElseThrow();
    }

    private String send(String method, String path, Object body) throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(apiUrl + path))
                .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("%s %s", method, path).isBetween(200, 299);
        return response.body();
    }

    private JsonNode json(String method, String path, Object body) throws Exception {
        String content = send(method, path, body);
        return content.isBlank() ? null : mapper.readTree(content);
    }

    private int project(String name) throws Exception {
        return json("POST", "/api/scrum-project", Map.of("name", name)).path("projectID").asInt();
    }

    private int sprint(int project, String name) throws Exception {
        return json("POST", "/api/sprint", Map.of("projectID", project, "name", name)).path("id").asInt();
    }

    private int issue(int project, String name, Integer points) throws Exception {
        return json("POST", "/api/issue", mapper.createObjectNode().put("projectID", project)
                .put("name", name).put("estimation", points).put("issueTypeID", 1)).path("issueID").asInt();
    }

    private void attach(int sprint, int issue) throws Exception {
        json("POST", "/api/sprint/" + sprint + "/issue/" + issue, null);
    }

    private void complete(int issue) throws Exception {
        for (int status = 2; status <= 5; status++) {
            json("PATCH", "/api/issue/" + issue + "/status/" + status, null);
        }
    }

    private void reestimate(int issue, String name, int points) throws Exception {
        json("PUT", "/api/issue", Map.of("issueID", issue, "name", name, "estimation", points));
    }

    private void verifyResult(JsonNode report, int done, int incomplete, long velocity, long incompletePoints) {
        assertThat(report.path("completedIssueCount").asInt()).isEqualTo(done);
        assertThat(report.path("incompleteIssueCount").asInt()).isEqualTo(incomplete);
        assertThat(report.path("velocity").asLong()).isEqualTo(velocity);
        assertThat(report.path("incompletePoints").asLong()).isEqualTo(incompletePoints);
    }

    private void verifyCharts(int sprint, JsonNode report) throws Exception {
        JsonNode down = json("GET", "/api/reports/sprints/" + sprint + "/burndown", null);
        JsonNode up = json("GET", "/api/reports/sprints/" + sprint + "/burnup", null);
        assertThat(up.size()).isPositive().isEqualTo(down.size());
        List<Instant> times = StreamSupport.stream(up.spliterator(), false)
                .map(point -> Instant.parse(point.path("timestamp").asText())).toList();
        assertThat(times).isSorted();
        for (int index = 0; index < up.size(); index++) {
            assertThat(down.get(index).path("timestamp")).isEqualTo(up.get(index).path("timestamp"));
            long remaining = up.get(index).path("totalPoints").asLong() - up.get(index).path("completedPoints").asLong();
            assertThat(down.get(index).path("remainingPoints").asLong()).isEqualTo(remaining).isNotNegative();
        }
        JsonNode last = up.get(up.size() - 1);
        assertThat(last.path("completedPoints")).isEqualTo(report.path("velocity"));
        assertThat(last.path("totalPoints").asLong())
                .isEqualTo(report.path("velocity").asLong() + report.path("incompletePoints").asLong());
    }
}
