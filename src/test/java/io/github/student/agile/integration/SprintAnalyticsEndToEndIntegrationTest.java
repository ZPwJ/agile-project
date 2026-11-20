package io.github.student.agile.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.student.agile.AgileServerApplication;
import io.github.student.agile.resource.domain.dictionary.IssuePriorityID;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.domain.dictionary.IssueTypeID;
import io.github.student.agile.resource.dto.ProjectDTO;
import io.github.student.agile.resource.dto.SprintDTO;
import io.github.student.agile.resource.dto.request.IssueMRO;
import io.github.student.agile.security.JwtAuthConverter;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AgileServerApplication.class)
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "jwt.auth.converter.resource-id=agile-project-server"
})
public class SprintAnalyticsEndToEndIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtAuthConverter jwtAuthConverter;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    public void fullProjectAndTwoSprintsLifecycleProducesConsistentAnalyticsAndCsvExport() throws Exception {
        RequestPostProcessor scrumMaster = jwtAuth("scrum.master@agile.local", List.of("sm", "client_user"));
        RequestPostProcessor outsider = jwtAuth("outsider@agile.local", List.of("dev", "client_user"));

        // 1. Create project
        ProjectDTO createProjectReq = ProjectDTO.builder()
                .name("E2E Analytics Project")
                .description("End-to-end sprint analytics lifecycle test")
                .projectGoal("Verify all analytics endpoints")
                .build();
        MvcResult projectResult = mvc.perform(post("/api/scrum-project")
                        .with(scrumMaster)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createProjectReq)))
                .andExpect(status().isCreated())
                .andReturn();
        Integer projectId = readJson(projectResult).get("projectID").asInt();

        // 2. Create backlog issues (estimated + unestimated)
        Integer issue1Id = createIssue(scrumMaster, projectId, "Auth module", 8);
        Integer issue2Id = createIssue(scrumMaster, projectId, "Unestimated docs task", null);
        Integer issue3Id = createIssue(scrumMaster, projectId, "Reporting service", 5);
        Integer issue4Id = createIssue(scrumMaster, projectId, "CSV export", 3);

        // 3. Sprint 1 lifecycle: create -> assign 3 issues -> start -> complete 2 issues (8 SP + null) -> stop
        Integer sprint1Id = createSprint(scrumMaster, projectId, "Sprint 1, \"Alpha\"", "Deliver MVP");
        addIssueToSprint(scrumMaster, sprint1Id, issue1Id);
        addIssueToSprint(scrumMaster, sprint1Id, issue2Id);
        addIssueToSprint(scrumMaster, sprint1Id, issue3Id);

        mvc.perform(patch("/api/sprint/start/{id}", sprint1Id).with(scrumMaster))
                .andExpect(status().isOk());

        moveIssueToDone(scrumMaster, issue1Id);
        moveIssueToDone(scrumMaster, issue2Id);
        // Move issue3 only to IN_PROGRESS so it remains uncompleted when Sprint 1 stops
        mvc.perform(patch("/api/issue/{id}/status/{statusId}", issue3Id, IssueStatusID.IN_PROGRESS.getStatusID())
                        .with(scrumMaster))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/sprint/project/{projectId}/stop", projectId).with(scrumMaster))
                .andExpect(status().isOk());

        // 4. Sprint 2 lifecycle (active sprint with carried-over issue3 + issue4, velocity = 0 so far)
        Integer sprint2Id = createSprint(scrumMaster, projectId, "Sprint 2", "Continue delivery");
        addIssueToSprint(scrumMaster, sprint2Id, issue3Id);
        addIssueToSprint(scrumMaster, sprint2Id, issue4Id);

        mvc.perform(patch("/api/sprint/start/{id}", sprint2Id).with(scrumMaster))
                .andExpect(status().isOk());

        // 5. Verify WF-1: Sprint 1 summary report
        mvc.perform(get("/api/analytics/sprints/{sprintId}/report", sprint1Id).with(scrumMaster))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId", is(sprint1Id)))
                .andExpect(jsonPath("$.sprintName", is("Sprint 1, \"Alpha\"")))
                .andExpect(jsonPath("$.projectId", is(projectId)))
                .andExpect(jsonPath("$.startDate", notNullValue()))
                .andExpect(jsonPath("$.endDate", notNullValue()))
                .andExpect(jsonPath("$.initialIssueCount", is(3)))
                .andExpect(jsonPath("$.completedIssueCount", is(2)))
                .andExpect(jsonPath("$.uncompletedIssueCount", is(1)))
                .andExpect(jsonPath("$.velocity", is(8)))
                .andExpect(jsonPath("$.uncompletedEstimationSum", is(5)))
                .andExpect(jsonPath("$.estimationUnit", is("STORY_POINTS")));

        // 6. Verify WF-3: Sprint 1 burndown chart
        mvc.perform(get("/api/analytics/sprints/{sprintId}/burndown", sprint1Id).with(scrumMaster))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId", is(sprint1Id)))
                .andExpect(jsonPath("$.unit", is("STORY_POINTS")))
                .andExpect(jsonPath("$.points", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.points[0].remainingWork", is(5)))
                .andExpect(jsonPath("$.points[0].unit", is("STORY_POINTS")));

        // 7. Verify WF-4: Sprint 1 burnup chart
        mvc.perform(get("/api/analytics/sprints/{sprintId}/burnup", sprint1Id).with(scrumMaster))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintId", is(sprint1Id)))
                .andExpect(jsonPath("$.unit", is("STORY_POINTS")))
                .andExpect(jsonPath("$.points", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.points[0].completedWork", is(8)))
                .andExpect(jsonPath("$.points[0].totalScope", is(13)))
                .andExpect(jsonPath("$.points[0].unit", is("STORY_POINTS")));

        // 8. Verify WF-5 & WF-6: Project sprints comparison (unfiltered, filtered, invalid date range)
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        mvc.perform(get("/api/analytics/projects/{projectId}/sprints", projectId)
                        .param("from", todayUtc.minusDays(1).toString())
                        .param("to", todayUtc.plusDays(1).toString())
                        .with(scrumMaster))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].projectId", is(projectId)))
                .andExpect(jsonPath("$[0].sprintId", is(sprint1Id)))
                .andExpect(jsonPath("$[0].completedIssueCount", is(2)))
                .andExpect(jsonPath("$[0].uncompletedIssueCount", is(1)))
                .andExpect(jsonPath("$[0].velocity", is(8)))
                .andExpect(jsonPath("$[1].projectId", is(projectId)))
                .andExpect(jsonPath("$[1].sprintId", is(sprint2Id)))
                .andExpect(jsonPath("$[1].endDate", nullValue()))
                .andExpect(jsonPath("$[1].completedIssueCount", is(0)))
                .andExpect(jsonPath("$[1].uncompletedIssueCount", is(2)))
                .andExpect(jsonPath("$[1].velocity", is(0)));

        mvc.perform(get("/api/analytics/projects/{projectId}/sprints", projectId)
                        .param("from", "2026-12-31")
                        .param("to", "2026-01-01")
                        .with(scrumMaster))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.trace").doesNotExist());

        // 9. Verify WF-7: CSV export (RFC 4180 escaping + date filter + empty range)
        MvcResult csvResult = mvc.perform(get("/api/analytics/projects/{projectId}/sprints/export", projectId)
                        .param("from", todayUtc.minusDays(1).toString())
                        .param("to", todayUtc.plusDays(1).toString())
                        .with(scrumMaster))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("project-" + projectId + "-sprints.csv")))
                .andReturn();

        String csvBody = csvResult.getResponse().getContentAsString();
        String[] lines = csvBody.split("\r\n");
        assertEquals(3, lines.length);
        assertEquals("projectId,sprintId,sprintName,startDate,endDate,completedIssueCount,uncompletedIssueCount,velocity", lines[0]);
        assertTrue(lines[1].contains("\"Sprint 1, \"\"Alpha\"\"\""));
        assertTrue(lines[1].endsWith(",2,1,8"));
        assertTrue(lines[2].contains(",Sprint 2,"));
        assertTrue(lines[2].endsWith(",0,2,0"));

        // Empty range export returns header only
        mvc.perform(get("/api/analytics/projects/{projectId}/sprints/export", projectId)
                        .param("to", "2020-01-01")
                        .with(scrumMaster))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "projectId,sprintId,sprintName,startDate,endDate,completedIssueCount,uncompletedIssueCount,velocity\r\n"
                ));

        // 10. Verify project access isolation (outsider gets 403)
        mvc.perform(get("/api/analytics/sprints/{sprintId}/report", sprint1Id).with(outsider))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
        mvc.perform(get("/api/analytics/projects/{projectId}/sprints/export", projectId).with(outsider))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    private Integer createIssue(RequestPostProcessor auth, Integer projectId, String name, Integer estimation) throws Exception {
        IssueMRO request = IssueMRO.builder()
                .name(name)
                .description("Description for " + name)
                .estimation(estimation)
                .projectID(projectId)
                .issueTypeID(IssueTypeID.USER_STORY.getTypeID())
                .issuePriorityID(IssuePriorityID.MEDIUM.getPriorityID())
                .build();
        MvcResult result = mvc.perform(post("/api/issue")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("issueID").asInt();
    }

    private Integer createSprint(RequestPostProcessor auth, Integer projectId, String name, String goal) throws Exception {
        SprintDTO request = SprintDTO.builder()
                .name(name)
                .sprintGoal(goal)
                .projectID(projectId)
                .build();
        MvcResult result = mvc.perform(post("/api/sprint")
                        .with(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asInt();
    }

    private void addIssueToSprint(RequestPostProcessor auth, Integer sprintId, Integer issueId) throws Exception {
        mvc.perform(post("/api/sprint/{sprintId}/issue/{issueId}", sprintId, issueId).with(auth))
                .andExpect(status().isOk());
    }

    private void moveIssueToDone(RequestPostProcessor auth, Integer issueId) throws Exception {
        int[] statuses = {
                IssueStatusID.IN_PROGRESS.getStatusID(),
                IssueStatusID.CODE_REVIEW.getStatusID(),
                IssueStatusID.IN_TESTS.getStatusID(),
                IssueStatusID.DONE.getStatusID()
        };
        for (int statusId : statuses) {
            mvc.perform(patch("/api/issue/{id}/status/{statusId}", issueId, statusId).with(auth))
                    .andExpect(status().isOk());
        }
    }

    private RequestPostProcessor jwtAuth(String username, List<String> clientRoles) {
        Jwt jwt = Jwt.withTokenValue("test-token-" + username)
                .header("alg", "none")
                .subject(username)
                .claim("preferred_username", username)
                .claim("email", username)
                .claim("given_name", "Test")
                .claim("family_name", "User")
                .claim("resource_access", Map.of("agile-project-server", Map.of("roles", clientRoles)))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        AbstractAuthenticationToken authToken = jwtAuthConverter.convert(jwt);
        return authentication(authToken);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
