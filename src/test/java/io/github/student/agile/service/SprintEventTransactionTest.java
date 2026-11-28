package io.github.student.agile.service;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.IssueService;
import io.github.student.agile.resource.service.SprintService;
import io.github.student.agile.resource.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static io.github.student.agile.builder.IssueBuilder.createIssueEntity;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static io.github.student.agile.builder.SprintBuilder.createSprintEntity;
import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;

/**
 * Not transactional on purpose: only then it can be seen that the service rolled its change back.
 */
@SpringBootTest(classes = TestConfiguration.class)
public class SprintEventTransactionTest {

    private static final String EVENT_FAILURE = "event cannot be saved";

    @Autowired
    private SprintService sprintService;

    @Autowired
    private IssueService issueService;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueStatusRepository issueStatusRepository;

    @MockBean
    private SprintIssueEventRepository sprintIssueEventRepository;

    @MockBean
    private UserService userService;

    private Project project;
    private Sprint sprint;
    private Issue issue;

    @BeforeEach
    void setUp() {

        Mockito.when(userService.getCurrentLoggedUser()).thenReturn(createCurrentLoggedInUser());
        Mockito.when(sprintIssueEventRepository.save(any())).thenThrow(new IllegalStateException(EVENT_FAILURE));
        Mockito.when(sprintIssueEventRepository.saveAll(anyIterable())).thenThrow(new IllegalStateException(EVENT_FAILURE));
        project = projectRepository.save(createProjectEntity());
    }

    @AfterEach
    void cleanUp() {

        issueRepository.deleteById(issue.getId());
        sprintRepository.deleteById(sprint.getId());
        projectRepository.deleteById(project.getId());
    }

    @Test
    public void startSprintIsRolledBackWhenEventCannotBeSaved() {

        //given
        sprint = sprintRepository.save(createSprintEntity(project, Boolean.FALSE));
        issue = saveIssueInSprint(IssueStatusID.TO_DO);

        //when
        Exception exception = assertThrows(IllegalStateException.class, () -> sprintService.startSprint(sprint.getId()));

        //then
        assertEquals(EVENT_FAILURE, exception.getMessage());
        Sprint saved = sprintRepository.findById(sprint.getId()).orElseThrow();
        assertFalse(saved.getActive());
        assertNull(saved.getStartedAt());
    }

    @Test
    public void changeIssueStatusIsRolledBackWhenEventCannotBeSaved() {

        //given
        sprint = sprintRepository.save(createSprintEntity(project, Boolean.TRUE));
        issue = saveIssueInSprint(IssueStatusID.IN_TESTS);

        //when
        Exception exception = assertThrows(IllegalStateException.class,
                () -> issueService.changeIssueStatus(issue.getId(), IssueStatusID.DONE.getStatusID()));

        //then
        assertEquals(EVENT_FAILURE, exception.getMessage());
        Issue saved = issueRepository.findById(issue.getId()).orElseThrow();
        assertEquals(IssueStatusID.IN_TESTS.getStatusID(), saved.getIssueStatus().getId());
    }

    private Issue saveIssueInSprint(IssueStatusID status) {

        Issue newIssue = createIssueEntity();
        newIssue.setProjectID(project.getId());
        newIssue.setSprint(sprint);
        newIssue.setIssueStatus(issueStatusRepository.findById(status.getStatusID()).orElseThrow());
        return issueRepository.save(newIssue);
    }
}
