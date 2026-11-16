package io.github.student.agile.integration;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.controller.ApiExceptionHandler;
import io.github.student.agile.resource.controller.IssueRestController;
import io.github.student.agile.resource.domain.Backlog;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.IssueTypeRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.service.IssueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
class BacklogHistoryIntegrationTest {

    @Autowired private IssueService issueService;
    @Autowired private ProjectRepository projects;
    @Autowired private IssueRepository issues;
    @Autowired private IssueStatusRepository statuses;
    @Autowired private IssueTypeRepository types;

    private Project project;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        project = project("Backlog history");
        mvc = MockMvcBuilders.standaloneSetup(new IssueRestController(issueService))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void returnsOnlyDoneIssuesOfTheProjectRegardlessOfOptionalType() throws Exception {
        Issue done = issue(project, 5, 2);
        Issue doneWithoutType = issue(project, 5, null);
        for (int status = 1; status <= 4; status++) {
            issue(project, status, status % 2 == 0 ? 1 : null);
        }
        issue(project, null, null);
        issue(project("Other project"), 5, 2);

        mvc.perform(get("/api/issue/product-backlog-history").param("projectID", project.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(done.getId(), doneWithoutType.getId())))
                .andExpect(jsonPath("$[*].issueStatusID", containsInAnyOrder(5, 5)));
    }

    @Test
    void returnsEmptyListForEmptyHistoryAnd404ForMissingProject() throws Exception {
        mvc.perform(get("/api/issue/product-backlog-history").param("projectID", project.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/issue/product-backlog-history").param("projectID", "2147483647"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project with given ID does not exists"));
    }

    private Project project(String name) {
        return projects.save(Project.builder().name(name).backlog(new Backlog()).build());
    }

    private Issue issue(Project owner, Integer status, Integer type) {
        Issue issue = issues.save(Issue.builder().name("History task").projectID(owner.getId())
                .issueStatus(status == null ? null : statuses.getReferenceById(status))
                .issueType(type == null ? null : types.getReferenceById(type)).build());
        owner.getBacklog().getIssues().add(issue);
        return issue;
    }
}
