package io.github.student.agile.integration;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Loads data-demo.sql through the real `demo` profile and checks the reports documented for it.
 * H2 runs in PostgreSQL mode because the script uses ON CONFLICT DO NOTHING, like data-prod.sql.
 */
@EnableJpaAuditing
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@SpringBootTest(classes = {TestConfiguration.class},
        properties = "spring.datasource.url=jdbc:h2:mem:demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
public class DemoDataIntegrationTest {

    private static final String ANALYTICS_BASE_URL = "/api/analytics";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @Test
    public void firstSprint_hasIssueWithoutEstimationAndScopeChange() throws Exception {
        mvc.perform(get(ANALYTICS_BASE_URL + "/sprints/{id}/report", 9001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("FINISHED")))
                .andExpect(jsonPath("$.historyAvailable", is(true)))
                .andExpect(jsonPath("$.initialIssueCount", is(5)))
                .andExpect(jsonPath("$.completedIssueCount", is(5)))
                .andExpect(jsonPath("$.uncompletedIssueCount", is(1)))
                .andExpect(jsonPath("$.velocity", is(18)))
                .andExpect(jsonPath("$.uncompletedEstimation", is(5)));

        mvc.perform(get(ANALYTICS_BASE_URL + "/sprints/{id}/burndown", 9001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialScope", is(18)))
                .andExpect(jsonPath("$.points[*].remainingWork", contains(18, 18, 13, 13, 8, 0)))
                .andExpect(jsonPath("$.points[*].remainingScope", contains(18, 18, 15, 18, 13, 5)));

        mvc.perform(get(ANALYTICS_BASE_URL + "/sprints/{id}/burnup", 9001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[*].completedWork", contains(0, 0, 5, 5, 10, 18)))
                .andExpect(jsonPath("$.points[*].totalScope", contains(18, 18, 20, 23, 23, 23)));
    }

    @Test
    public void secondSprint_ignoresRemovedIssue() throws Exception {
        mvc.perform(get(ANALYTICS_BASE_URL + "/sprints/{id}/report", 9002))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialIssueCount", is(4)))
                .andExpect(jsonPath("$.completedIssueCount", is(3)))
                .andExpect(jsonPath("$.uncompletedIssueCount", is(1)))
                .andExpect(jsonPath("$.velocity", is(7)))
                .andExpect(jsonPath("$.uncompletedEstimation", is(8)));
    }

    @Test
    public void projectComparison_listsBothSprintsAndRespectsDateFilter() throws Exception {
        mvc.perform(get(ANALYTICS_BASE_URL + "/projects/{id}/sprints", 9001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprints[*].sprintID", contains(9001, 9002)))
                .andExpect(jsonPath("$.sprints[*].velocity", contains(18, 7)));

        mvc.perform(get(ANALYTICS_BASE_URL + "/projects/{id}/sprints", 9001).param("from", "2026-10-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprints", hasSize(1)))
                .andExpect(jsonPath("$.sprints[0].sprintID", is(9002)));
    }
}
