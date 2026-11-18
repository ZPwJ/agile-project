package io.github.student.agile.integration;

import io.github.student.agile.exception.definition.ProjectAccessDeniedException;
import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.exception.handler.GlobalExceptionHandler;
import io.github.student.agile.resource.controller.SprintAnalyticsRestController;
import io.github.student.agile.resource.dto.SprintSummaryReportDTO;
import io.github.student.agile.resource.service.SprintAnalyticsService;
import io.github.student.agile.security.JwtAuthConverter;
import io.github.student.agile.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.ACTION_NOT_ALLOWED;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SprintAnalyticsRestController.class)
@Import({SecurityConfig.class, JwtAuthConverter.class, GlobalExceptionHandler.class})
public class SprintAnalyticsRestControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SprintAnalyticsService sprintAnalyticsService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    public void getSprintSummaryReport_returns200AndReportPayloadForActiveSprintWithoutEndDate() throws Exception {
        ZonedDateTime startDate = ZonedDateTime.of(2026, 11, 10, 9, 0, 0, 0, ZoneOffset.UTC);
        SprintSummaryReportDTO report = SprintSummaryReportDTO.builder()
                .sprintId(1)
                .sprintName("Sprint 1")
                .projectId(10)
                .startDate(startDate)
                .endDate(null)
                .initialIssueCount(4)
                .completedIssueCount(2)
                .uncompletedIssueCount(2)
                .velocity(0)
                .uncompletedEstimationSum(0)
                .estimationUnit("STORY_POINTS")
                .build();

        when(sprintAnalyticsService.getSprintSummaryReport(1)).thenReturn(report);

        mvc.perform(get("/api/analytics/sprints/1/report").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sprintId", is(1)))
                .andExpect(jsonPath("$.sprintName", is("Sprint 1")))
                .andExpect(jsonPath("$.projectId", is(10)))
                .andExpect(jsonPath("$.startDate", notNullValue()))
                .andExpect(jsonPath("$.endDate", nullValue()))
                .andExpect(jsonPath("$.initialIssueCount", is(4)))
                .andExpect(jsonPath("$.completedIssueCount", is(2)))
                .andExpect(jsonPath("$.uncompletedIssueCount", is(2)))
                .andExpect(jsonPath("$.velocity", is(0)))
                .andExpect(jsonPath("$.uncompletedEstimationSum", is(0)))
                .andExpect(jsonPath("$.estimationUnit", is("STORY_POINTS")));
    }

    @Test
    public void getSprintSummaryReport_returns404WhenSprintDoesNotExist() throws Exception {
        when(sprintAnalyticsService.getSprintSummaryReport(999))
                .thenThrow(new ResourceNotFoundException(SPRINT_DOES_NOT_EXISTS));

        mvc.perform(get("/api/analytics/sprints/999/report").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is(SPRINT_DOES_NOT_EXISTS)))
                .andExpect(jsonPath("$.path", is("/api/analytics/sprints/999/report")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void getSprintSummaryReport_returns403WhenUserHasNoProjectAccess() throws Exception {
        when(sprintAnalyticsService.getSprintSummaryReport(5))
                .thenThrow(new ProjectAccessDeniedException(ACTION_NOT_ALLOWED));

        mvc.perform(get("/api/analytics/sprints/5/report").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is(ACTION_NOT_ALLOWED)))
                .andExpect(jsonPath("$.path", is("/api/analytics/sprints/5/report")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void getSprintSummaryReport_returns401WithoutJwt() throws Exception {
        mvc.perform(get("/api/analytics/sprints/1/report"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Authentication required")))
                .andExpect(jsonPath("$.path", is("/api/analytics/sprints/1/report")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void getSprintSummaryReport_returns400ForInvalidSprintIdType() throws Exception {
        mvc.perform(get("/api/analytics/sprints/not-a-number/report").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.path", is("/api/analytics/sprints/not-a-number/report")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
