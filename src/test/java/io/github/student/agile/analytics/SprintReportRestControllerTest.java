
package io.github.student.agile.analytics;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.analytics.reports.SprintComparisonService;
import io.github.student.agile.resource.analytics.reports.SprintComparisonCsvService;
import io.github.student.agile.resource.controller.analytics.SprintReportRestController;
import io.github.student.agile.resource.analytics.error.ProjectNotFoundException;
import io.github.student.agile.resource.dto.SprintComparisonRowDTO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {TestConfiguration.class}
)
class SprintReportRestControllerTest {

    private static final String BASE =
            "/api/analytics/projects/1/sprints";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SprintComparisonService comparisonService;

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    void comparisonReturnsEmptyJsonArray() throws Exception {
        when(comparisonService.compareRows(1, null, null))
                .thenReturn(List.of());

        mvc.perform(get(BASE + "/comparison"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    void comparisonReturnsSprintData() throws Exception {
        when(comparisonService.compareRows(1, null, null))
                .thenReturn(List.of(
                        new SprintComparisonRowDTO(
                                10, "Sprint testowy", 1,
                                null, null, null,
                                3, 2, 15L, null
                        )
                ));

        mvc.perform(get(BASE + "/comparison"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sprintId", is(10)))
                .andExpect(jsonPath("$[0].sprintName", is("Sprint testowy")))
                .andExpect(jsonPath("$[0].completedTasks", is(3)))
                .andExpect(jsonPath("$[0].velocity", is(15)));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    void comparisonPassesDateFilters() throws Exception {
        when(comparisonService.compareRows(
                1, "2026-01-01", "2026-03-31"))
                .thenReturn(List.of());

        mvc.perform(get(BASE + "/comparison")
                        .param("from", "2026-01-01")
                        .param("to", "2026-03-31"))
                .andExpect(status().isOk());

        verify(comparisonService).compareRows(
                1, "2026-01-01", "2026-03-31");
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    void comparisonUnknownProjectReturns404() throws Exception {
        when(comparisonService.compareRows(1, null, null))
                .thenThrow(new ProjectNotFoundException(1));

        mvc.perform(get(BASE + "/comparison"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    void csvHasCorrectContentType() throws Exception {
        when(comparisonService.compareRows(1, null, null))
                .thenReturn(List.of());

        mvc.perform(get(BASE + "/comparison.csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    void csvHasDownloadFilename() throws Exception {
        when(comparisonService.compareRows(1, null, null))
                .thenReturn(List.of());

        mvc.perform(get(BASE + "/comparison.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"sprint-comparison-1.csv\""
                ));
    }

    @Test
    @WithMockUser(username = "dev", roles = "dev")
    void emptyCsvContainsHeader() throws Exception {
        when(comparisonService.compareRows(1, null, null))
                .thenReturn(List.of());

        mvc.perform(get(BASE + "/comparison.csv"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "projectId,sprintId,sprintName"
                )));
    }
}
