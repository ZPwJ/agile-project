package io.github.student.agile.integration;

import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.controller.ApiExceptionHandler;
import io.github.student.agile.resource.controller.SprintReportRestController;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintProgress;
import io.github.student.agile.resource.dto.SprintReportDTO;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintProgressRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.SprintReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
class SprintComparisonIntegrationTest {

    @Autowired private SprintReportService reports;
    @Autowired private ProjectRepository projects;
    @Autowired private SprintRepository sprints;
    @Autowired private SprintProgressRepository progress;

    private Project project;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        project = projects.save(Project.builder().name("Comparison").build());
        mvc = MockMvcBuilders.standaloneSetup(new SprintReportRestController(reports))
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new ByteArrayHttpMessageConverter(), new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build()))
                .build();
    }

    @Test
    void listsOnlyClosedProjectSprintsInStableOrderAndMatchesIndividualReports() throws Exception {
        Sprint later = closed(project, "Later", "2026-11-10T12:00:00Z", 8, 3);
        Sprint first = closed(project, "Zero velocity", "2026-11-01T12:00:00Z", 5, 0);
        Sprint tied = closed(project, "Same end", "2026-11-01T12:00:00Z", 13, 8);
        closed(projects.save(Project.builder().name("Other").build()), "Other", "2026-11-01T12:00:00Z", 100, 100);
        sprints.save(Sprint.builder().name("Active").project(project).active(true).isDone(false).build());
        sprints.save(Sprint.builder().name("Planned").project(project).active(false).isDone(false).build());

        List<SprintReportDTO> result = reports.getComparison(project.getId(), null, null);

        assertThat(result).containsExactly(reports.getReport(first.getId()), reports.getReport(tied.getId()),
                reports.getReport(later.getId()));
        mvc.perform(get("/api/reports/sprints").param("projectID", project.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].sprintID").value(first.getId()))
                .andExpect(jsonPath("$[0].name").value("Zero velocity"))
                .andExpect(jsonPath("$[0].projectID").value(project.getId()))
                .andExpect(jsonPath("$[0].startDate").value(first.getStartDate().toInstant().toString()))
                .andExpect(jsonPath("$[0].endDate").value(first.getEndDate().toInstant().toString()))
                .andExpect(jsonPath("$[0].completedIssueCount").value(0))
                .andExpect(jsonPath("$[0].incompleteIssueCount").value(2))
                .andExpect(jsonPath("$[0].velocity").value(0));
    }

    @Test
    void filtersByUtcEndDateWithInclusiveAndOptionalBounds() throws Exception {
        Sprint before = closed(project, "Before", "2026-11-01T01:00:00+02:00", 5, 0);
        Sprint lower = closed(project, "Lower", "2026-11-01T00:00:00Z", 5, 0);
        Sprint upper = closed(project, "Upper", "2026-11-01T23:59:59.999999Z", 5, 0);
        Sprint after = closed(project, "After", "2026-11-02T00:00:00Z", 5, 0);
        LocalDate date = LocalDate.of(2026, 11, 1);

        assertThat(reports.getComparison(project.getId(), date, date)).extracting(SprintReportDTO::sprintID)
                .containsExactly(lower.getId(), upper.getId());
        assertThat(reports.getComparison(project.getId(), date, null)).extracting(SprintReportDTO::sprintID)
                .containsExactly(lower.getId(), upper.getId(), after.getId());
        assertThat(reports.getComparison(project.getId(), null, date)).extracting(SprintReportDTO::sprintID)
                .containsExactly(before.getId(), lower.getId(), upper.getId());
        mvc.perform(get("/api/reports/sprints").param("projectID", project.getId().toString())
                        .param("from", date.toString()).param("to", date.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sprintID").value(lower.getId()))
                .andExpect(jsonPath("$[1].sprintID").value(upper.getId()));
    }

    @Test
    void emptyProjectAndUnmatchedRangeReturnEmptyLists() throws Exception {
        mvc.perform(get("/api/reports/sprints").param("projectID", project.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        closed(project, "Outside range", "2026-11-01T12:00:00Z", 5, 0);
        mvc.perform(get("/api/reports/sprints").param("projectID", project.getId().toString())
                        .param("from", "2026-12-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void csvUsesComparisonFiltersAndReturnsUtf8AttachmentWithEscapedNames() throws Exception {
        closed(project, "Outside range", "2026-10-31T23:59:59Z", 5, 0);
        Sprint included = closed(project, "Sprint, \"Zażółć\"\nNowa linia", "2026-11-01T12:00:00Z", 8, 3);
        closed(project, "After range", "2026-11-02T00:00:00Z", 5, 0);
        closed(projects.save(Project.builder().name("Other CSV project").build()),
                "Other", "2026-11-01T12:00:00Z", 100, 100);
        SprintReportDTO row = reports.getComparison(project.getId(),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1)).getFirst();
        assertThat(row.sprintID()).isEqualTo(included.getId());

        String csv = mvc.perform(get("/api/reports/sprints/csv").accept("text/csv")
                        .param("projectID", project.getId().toString())
                        .param("from", "2026-11-01").param("to", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"sprint-report-" + project.getId() + ".csv\""))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv).isEqualTo(
                "projectID,sprintID,name,startDate,endDate,completedIssueCount,incompleteIssueCount,velocity\r\n"
                        + row.projectID() + "," + row.sprintID() + ",\"Sprint, \"\"Zażółć\"\"\nNowa linia\","
                        + row.startDate() + "," + row.endDate() + "," + row.completedIssueCount() + ","
                        + row.incompleteIssueCount() + "," + row.velocity() + "\r\n");
    }

    @Test
    void emptyCsvHasOnlyHeaderAndInvalidRequestsReturnJsonErrors() throws Exception {
        String csv = mvc.perform(get("/api/reports/sprints/csv").param("projectID", project.getId().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).isEqualTo(
                "projectID,sprintID,name,startDate,endDate,completedIssueCount,incompleteIssueCount,velocity\r\n");
        mvc.perform(get("/api/reports/sprints/csv").param("projectID", "2147483647"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project with given ID does not exists"));
        mvc.perform(get("/api/reports/sprints/csv").param("projectID", project.getId().toString())
                        .param("from", "2026-11-02").param("to", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must not be after to"));
        mvc.perform(get("/api/reports/sprints/csv").param("projectID", project.getId().toString())
                        .param("to", "2026-02-30"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingEndDateSortsLastAndIsExcludedByDateFilters() {
        Sprint unknown = closed(project, "Unknown end", null, 5, 0);
        Sprint dated = closed(project, "Known end", "2026-11-01T12:00:00Z", 5, 0);

        assertThat(reports.getComparison(project.getId(), null, null)).extracting(SprintReportDTO::sprintID)
                .containsExactly(dated.getId(), unknown.getId());
        assertThat(reports.getComparison(project.getId(), LocalDate.of(2026, 11, 1), null))
                .extracting(SprintReportDTO::sprintID).containsExactly(dated.getId());
    }

    @Test
    void validatesProjectAndDatesWithReadableErrors() throws Exception {
        mvc.perform(get("/api/reports/sprints").param("projectID", "2147483647"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project with given ID does not exists"));
        mvc.perform(get("/api/reports/sprints")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/reports/sprints").param("projectID", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/reports/sprints").param("projectID", "text")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/reports/sprints").param("projectID", project.getId().toString())
                        .param("from", "2026-11-02").param("to", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must not be after to"));
        mvc.perform(get("/api/reports/sprints").param("projectID", project.getId().toString())
                        .param("from", "2026-02-30"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingFinalHistoryReturns409InsteadOfInventedMetrics() throws Exception {
        sprints.save(Sprint.builder().name("Missing result").project(project).isDone(true)
                .endDate(ZonedDateTime.parse("2026-11-01T12:00:00Z")).build());

        mvc.perform(get("/api/reports/sprints").param("projectID", project.getId().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Sprint has no recorded final result"));
    }

    private Sprint closed(Project owner, String name, String end, long total, long completed) {
        ZonedDateTime start = ZonedDateTime.parse("2026-10-01T12:00:00Z");
        ZonedDateTime finish = end == null ? null : ZonedDateTime.parse(end);
        Sprint sprint = sprints.save(Sprint.builder().name(name).project(owner).active(false).isDone(true)
                .startDate(start).endDate(finish).build());
        progress.save(SprintProgress.builder().sprint(sprint).kind(SprintProgress.Kind.START)
                .capturedAt(start).issueCount(2).totalPoints(total).build());
        progress.save(SprintProgress.builder().sprint(sprint).kind(SprintProgress.Kind.END)
                .capturedAt(finish == null ? start.plusDays(7) : finish).issueCount(2)
                .completedIssueCount(completed == 0 ? 0 : 1).totalPoints(total).completedPoints(completed).build());
        return sprint;
    }
}
