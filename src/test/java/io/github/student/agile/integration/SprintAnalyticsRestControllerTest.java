package io.github.student.agile.integration;

import io.github.student.agile.exception.definition.ProjectAccessDeniedException;
import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.exception.handler.GlobalExceptionHandler;
import io.github.student.agile.resource.controller.SprintAnalyticsRestController;
import io.github.student.agile.resource.dto.BurndownChartDTO;
import io.github.student.agile.resource.dto.BurndownPointDTO;
import io.github.student.agile.resource.dto.BurnupChartDTO;
import io.github.student.agile.resource.dto.BurnupPointDTO;
import io.github.student.agile.resource.dto.SprintComparisonItemDTO;
import io.github.student.agile.resource.dto.SprintSummaryReportDTO;
import io.github.student.agile.resource.service.SprintAnalyticsService;
import io.github.student.agile.security.JwtAuthConverter;
import io.github.student.agile.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.ACTION_NOT_ALLOWED;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.student.agile.exception.definition.InvalidDateRangeException;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.INVALID_DATE_RANGE;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @Test
    public void getBurnupChart_returns200AndChronologicalPoints() throws Exception {
        ZonedDateTime startDate = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime endDate = ZonedDateTime.of(2026, 11, 2, 17, 0, 0, 0, ZoneOffset.UTC);
        BurnupChartDTO chart = BurnupChartDTO.builder()
                .sprintId(1)
                .sprintName("Sprint 1")
                .projectId(10)
                .startDate(startDate)
                .endDate(endDate)
                .unit("STORY_POINTS")
                .points(List.of(
                        BurnupPointDTO.builder()
                                .date(LocalDate.of(2026, 11, 1))
                                .completedWork(0)
                                .totalScope(8)
                                .unit("STORY_POINTS")
                                .build(),
                        BurnupPointDTO.builder()
                                .date(LocalDate.of(2026, 11, 2))
                                .completedWork(5)
                                .totalScope(13)
                                .unit("STORY_POINTS")
                                .build()
                ))
                .build();

        when(sprintAnalyticsService.getBurnupChart(1)).thenReturn(chart);

        mvc.perform(get("/api/analytics/sprints/1/burnup").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sprintId", is(1)))
                .andExpect(jsonPath("$.sprintName", is("Sprint 1")))
                .andExpect(jsonPath("$.projectId", is(10)))
                .andExpect(jsonPath("$.unit", is("STORY_POINTS")))
                .andExpect(jsonPath("$.points", hasSize(2)))
                .andExpect(jsonPath("$.points[0].date", is("2026-11-01")))
                .andExpect(jsonPath("$.points[0].completedWork", is(0)))
                .andExpect(jsonPath("$.points[0].totalScope", is(8)))
                .andExpect(jsonPath("$.points[0].unit", is("STORY_POINTS")))
                .andExpect(jsonPath("$.points[1].date", is("2026-11-02")))
                .andExpect(jsonPath("$.points[1].completedWork", is(5)))
                .andExpect(jsonPath("$.points[1].totalScope", is(13)))
                .andExpect(jsonPath("$.points[1].unit", is("STORY_POINTS")));
    }

    @Test
    public void getBurnupChart_returns404WhenSprintDoesNotExist() throws Exception {
        when(sprintAnalyticsService.getBurnupChart(999))
                .thenThrow(new ResourceNotFoundException(SPRINT_DOES_NOT_EXISTS));

        mvc.perform(get("/api/analytics/sprints/999/burnup").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is(SPRINT_DOES_NOT_EXISTS)))
                .andExpect(jsonPath("$.path", is("/api/analytics/sprints/999/burnup")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void getBurnupChart_returns403WhenUserHasNoProjectAccess() throws Exception {
        when(sprintAnalyticsService.getBurnupChart(5))
                .thenThrow(new ProjectAccessDeniedException(ACTION_NOT_ALLOWED));

        mvc.perform(get("/api/analytics/sprints/5/burnup").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is(ACTION_NOT_ALLOWED)))
                .andExpect(jsonPath("$.path", is("/api/analytics/sprints/5/burnup")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void getBurnupChart_returns401WithoutJwt() throws Exception {
        mvc.perform(get("/api/analytics/sprints/1/burnup"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Authentication required")))
                .andExpect(jsonPath("$.path", is("/api/analytics/sprints/1/burnup")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void getBurnupChart_returns400ForInvalidSprintIdType() throws Exception {
        mvc.perform(get("/api/analytics/sprints/not-a-number/burnup").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.path", is("/api/analytics/sprints/not-a-number/burnup")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void getProjectSprintsComparison_returns200WithOrderedItemsIncludingZeroVelocity() throws Exception {
        ZonedDateTime start1 = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end1 = ZonedDateTime.of(2026, 11, 5, 17, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime start2 = ZonedDateTime.of(2026, 11, 6, 9, 0, 0, 0, ZoneOffset.UTC);

        List<SprintComparisonItemDTO> items = List.of(
                SprintComparisonItemDTO.builder()
                        .projectId(10)
                        .sprintId(1)
                        .sprintName("Sprint 1")
                        .startDate(start1)
                        .endDate(end1)
                        .completedIssueCount(3)
                        .uncompletedIssueCount(1)
                        .velocity(13)
                        .build(),
                SprintComparisonItemDTO.builder()
                        .projectId(10)
                        .sprintId(2)
                        .sprintName("Sprint 2")
                        .startDate(start2)
                        .endDate(null)
                        .completedIssueCount(0)
                        .uncompletedIssueCount(2)
                        .velocity(0)
                        .build()
        );

        when(sprintAnalyticsService.getProjectSprintsComparison(10, null, null)).thenReturn(items);

        mvc.perform(get("/api/analytics/projects/10/sprints").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].projectId", is(10)))
                .andExpect(jsonPath("$[0].sprintId", is(1)))
                .andExpect(jsonPath("$[0].sprintName", is("Sprint 1")))
                .andExpect(jsonPath("$[0].completedIssueCount", is(3)))
                .andExpect(jsonPath("$[0].uncompletedIssueCount", is(1)))
                .andExpect(jsonPath("$[0].velocity", is(13)))
                .andExpect(jsonPath("$[1].projectId", is(10)))
                .andExpect(jsonPath("$[1].sprintId", is(2)))
                .andExpect(jsonPath("$[1].sprintName", is("Sprint 2")))
                .andExpect(jsonPath("$[1].endDate", nullValue()))
                .andExpect(jsonPath("$[1].completedIssueCount", is(0)))
                .andExpect(jsonPath("$[1].uncompletedIssueCount", is(2)))
                .andExpect(jsonPath("$[1].velocity", is(0)));
    }

    @Test
    public void getProjectSprintsComparison_returns404WhenProjectDoesNotExist() throws Exception {
        when(sprintAnalyticsService.getProjectSprintsComparison(999, null, null))
                .thenThrow(new ResourceNotFoundException(PROJECT_DOES_NOT_EXISTS));

        mvc.perform(get("/api/analytics/projects/999/sprints").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is(PROJECT_DOES_NOT_EXISTS)))
                .andExpect(jsonPath("$.path", is("/api/analytics/projects/999/sprints")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void getProjectSprintsComparison_returns403WhenUserHasNoProjectAccess() throws Exception {
        when(sprintAnalyticsService.getProjectSprintsComparison(50,null,null))
                .thenThrow(new ProjectAccessDeniedException(ACTION_NOT_ALLOWED));

        mvc.perform(get("/api/analytics/projects/50/sprints").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is(ACTION_NOT_ALLOWED)))
                .andExpect(jsonPath("$.path", is("/api/analytics/projects/50/sprints")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void getProjectSprintsComparison_returns401WithoutJwt() throws Exception {
        mvc.perform(get("/api/analytics/projects/10/sprints"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Authentication required")))
                .andExpect(jsonPath("$.path", is("/api/analytics/projects/10/sprints")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void getProjectSprintsComparison_returns400ForInvalidProjectIdType() throws Exception {
        mvc.perform(get("/api/analytics/projects/not-a-number/sprints").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.path", is("/api/analytics/projects/not-a-number/sprints")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
    @Test
    public void getProjectSprintsComparison_parsesDateOnlyParamsAndPassesInclusiveRangeToService() throws Exception {
        when(sprintAnalyticsService.getProjectSprintsComparison(eq(10), any(), any())).thenReturn(List.of());

        mvc.perform(get("/api/analytics/projects/10/sprints")
                        .param("from", "2026-11-01")
                        .param("to", "2026-11-05")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        ArgumentCaptor<ZonedDateTime> fromCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
        ArgumentCaptor<ZonedDateTime> toCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
        verify(sprintAnalyticsService).getProjectSprintsComparison(eq(10), fromCaptor.capture(), toCaptor.capture());
        assertEquals(Instant.parse("2026-11-01T00:00:00Z"), fromCaptor.getValue().toInstant());
        assertEquals(Instant.parse("2026-11-05T23:59:59.999999999Z"), toCaptor.getValue().toInstant());
    }

    @Test
    public void getProjectSprintsComparison_parsesDateTimeParamsWithoutShiftingToEndOfDay() throws Exception {
        when(sprintAnalyticsService.getProjectSprintsComparison(eq(10), any(), any())).thenReturn(List.of());

        mvc.perform(get("/api/analytics/projects/10/sprints")
                        .param("from", "2026-11-01T09:00:00Z")
                        .param("to", "2026-11-05T17:00:00+01:00")
                        .with(jwt()))
                .andExpect(status().isOk());

        ArgumentCaptor<ZonedDateTime> fromCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
        ArgumentCaptor<ZonedDateTime> toCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
        verify(sprintAnalyticsService).getProjectSprintsComparison(eq(10), fromCaptor.capture(), toCaptor.capture());
        assertEquals(Instant.parse("2026-11-01T09:00:00Z"), fromCaptor.getValue().toInstant());
        assertEquals(Instant.parse("2026-11-05T16:00:00Z"), toCaptor.getValue().toInstant());
    }

    @Test
    public void getProjectSprintsComparison_acceptsOnlyOneBoundAndTreatsBlankParamsAsMissing() throws Exception {
        when(sprintAnalyticsService.getProjectSprintsComparison(eq(10), any(), any())).thenReturn(List.of());

        mvc.perform(get("/api/analytics/projects/10/sprints").param("from", "2026-11-01").with(jwt()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/analytics/projects/10/sprints").param("from", "").param("to", " ").with(jwt()))
                .andExpect(status().isOk());

        ArgumentCaptor<ZonedDateTime> fromCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
        ArgumentCaptor<ZonedDateTime> toCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
        verify(sprintAnalyticsService, org.mockito.Mockito.times(2))
                .getProjectSprintsComparison(eq(10), fromCaptor.capture(), toCaptor.capture());
        assertEquals(Instant.parse("2026-11-01T00:00:00Z"), fromCaptor.getAllValues().get(0).toInstant());
        assertNull(toCaptor.getAllValues().get(0));
        assertNull(fromCaptor.getAllValues().get(1));
        assertNull(toCaptor.getAllValues().get(1));
    }

    @Test
    public void getProjectSprintsComparison_returns400ForMalformedFromDateWithoutCallingService() throws Exception {
        mvc.perform(get("/api/analytics/projects/10/sprints").param("from", "not-a-date").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("'from'")))
                .andExpect(jsonPath("$.path", is("/api/analytics/projects/10/sprints")))
                .andExpect(jsonPath("$.trace").doesNotExist());

        verifyNoInteractions(sprintAnalyticsService);
    }

    @Test
    public void getProjectSprintsComparison_returns400ForMalformedToDate() throws Exception {
        mvc.perform(get("/api/analytics/projects/10/sprints")
                        .param("from", "2026-11-01")
                        .param("to", "2026-02-30")
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("'to'")))
                .andExpect(jsonPath("$.trace").doesNotExist());

        verifyNoInteractions(sprintAnalyticsService);
    }

    @Test
    public void getProjectSprintsComparison_returns400WhenFromIsAfterTo() throws Exception {
        when(sprintAnalyticsService.getProjectSprintsComparison(eq(10), any(), any()))
                .thenThrow(new InvalidDateRangeException(INVALID_DATE_RANGE));

        mvc.perform(get("/api/analytics/projects/10/sprints")
                        .param("from", "2026-11-10")
                        .param("to", "2026-11-01")
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is(INVALID_DATE_RANGE)))
                .andExpect(jsonPath("$.path", is("/api/analytics/projects/10/sprints")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void exportProjectSprintsComparisonCsv_returns200WithCsvHeadersAndBody() throws Exception {
        String csv = "projectId,sprintId,sprintName,startDate,endDate,completedIssueCount,uncompletedIssueCount,velocity\r\n"
                + "10,1,Sprint 1,2026-11-01T09:00:00Z,2026-11-05T17:00:00Z,3,1,13\r\n";

        when(sprintAnalyticsService.exportProjectSprintsComparisonCsv(eq(10), any(), any()))
                .thenReturn(csv.getBytes(StandardCharsets.UTF_8));

        mvc.perform(get("/api/analytics/projects/10/sprints/export")
                        .param("from", "2026-11-01")
                        .param("to", "2026-11-05")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("project-10-sprints.csv")))
                .andExpect(content().string(csv));

        ArgumentCaptor<ZonedDateTime> fromCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
        ArgumentCaptor<ZonedDateTime> toCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
        verify(sprintAnalyticsService).exportProjectSprintsComparisonCsv(eq(10), fromCaptor.capture(), toCaptor.capture());
        assertEquals(Instant.parse("2026-11-01T00:00:00Z"), fromCaptor.getValue().toInstant());
        assertEquals(Instant.parse("2026-11-05T23:59:59.999999999Z"), toCaptor.getValue().toInstant());
    }

    @Test
    public void exportProjectSprintsComparisonCsv_returns200WithHeaderOnlyForEmptyResult() throws Exception {
        String headerOnly = "projectId,sprintId,sprintName,startDate,endDate,completedIssueCount,uncompletedIssueCount,velocity\r\n";
        when(sprintAnalyticsService.exportProjectSprintsComparisonCsv(10, null, null))
                .thenReturn(headerOnly.getBytes(StandardCharsets.UTF_8));

        mvc.perform(get("/api/analytics/projects/10/sprints/export").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
                .andExpect(content().string(headerOnly));
    }

    @Test
    public void exportProjectSprintsComparisonCsv_returns400ForMalformedDateAndInvalidRange() throws Exception {
        mvc.perform(get("/api/analytics/projects/10/sprints/export")
                        .param("from", "bad-date")
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("'from'")))
                .andExpect(jsonPath("$.trace").doesNotExist());

        verifyNoInteractions(sprintAnalyticsService);

        when(sprintAnalyticsService.exportProjectSprintsComparisonCsv(eq(10), any(), any()))
                .thenThrow(new InvalidDateRangeException(INVALID_DATE_RANGE));

        mvc.perform(get("/api/analytics/projects/10/sprints/export")
                        .param("from", "2026-11-10")
                        .param("to", "2026-11-01")
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is(INVALID_DATE_RANGE)))
                .andExpect(jsonPath("$.path", is("/api/analytics/projects/10/sprints/export")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void exportProjectSprintsComparisonCsv_returns401_403_404Properly() throws Exception {
        mvc.perform(get("/api/analytics/projects/10/sprints/export"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(401)));

        when(sprintAnalyticsService.exportProjectSprintsComparisonCsv(50, null, null))
                .thenThrow(new ProjectAccessDeniedException(ACTION_NOT_ALLOWED));
        mvc.perform(get("/api/analytics/projects/50/sprints/export").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(403)));

        when(sprintAnalyticsService.exportProjectSprintsComparisonCsv(999, null, null))
                .thenThrow(new ResourceNotFoundException(PROJECT_DOES_NOT_EXISTS));
        mvc.perform(get("/api/analytics/projects/999/sprints/export").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    public void getBurndownChart_returns200WithChronologicalPoints() throws Exception {
        ZonedDateTime start = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime end = ZonedDateTime.of(2026, 11, 2, 17, 0, 0, 0, ZoneOffset.UTC);
        BurndownChartDTO chart = BurndownChartDTO.builder()
                .sprintId(1)
                .sprintName("Sprint 1")
                .projectId(10)
                .startDate(start)
                .endDate(end)
                .unit("STORY_POINTS")
                .points(List.of(
                        BurndownPointDTO.builder().date(LocalDate.of(2026, 11, 1)).remainingWork(13).unit("STORY_POINTS").build(),
                        BurndownPointDTO.builder().date(LocalDate.of(2026, 11, 2)).remainingWork(5).unit("STORY_POINTS").build()
                ))
                .build();

        when(sprintAnalyticsService.getBurndownChart(1)).thenReturn(chart);

        mvc.perform(get("/api/analytics/sprints/1/burndown").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sprintId", is(1)))
                .andExpect(jsonPath("$.sprintName", is("Sprint 1")))
                .andExpect(jsonPath("$.projectId", is(10)))
                .andExpect(jsonPath("$.unit", is("STORY_POINTS")))
                .andExpect(jsonPath("$.points", hasSize(2)))
                .andExpect(jsonPath("$.points[0].date", is("2026-11-01")))
                .andExpect(jsonPath("$.points[0].remainingWork", is(13)))
                .andExpect(jsonPath("$.points[0].unit", is("STORY_POINTS")))
                .andExpect(jsonPath("$.points[1].date", is("2026-11-02")))
                .andExpect(jsonPath("$.points[1].remainingWork", is(5)));
    }

    @Test
    public void getBurndownChart_returns400_401_403_404Properly() throws Exception {
        mvc.perform(get("/api/analytics/sprints/not-a-number/burndown").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.trace").doesNotExist());

        mvc.perform(get("/api/analytics/sprints/1/burndown"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(401)));

        when(sprintAnalyticsService.getBurndownChart(5))
                .thenThrow(new ProjectAccessDeniedException(ACTION_NOT_ALLOWED));
        mvc.perform(get("/api/analytics/sprints/5/burndown").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(403)));

        when(sprintAnalyticsService.getBurndownChart(999))
                .thenThrow(new ResourceNotFoundException(SPRINT_DOES_NOT_EXISTS));
        mvc.perform(get("/api/analytics/sprints/999/burndown").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is(SPRINT_DOES_NOT_EXISTS)));
    }
}
