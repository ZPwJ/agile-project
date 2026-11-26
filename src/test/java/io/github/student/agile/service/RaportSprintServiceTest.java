
package io.github.student.agile.service;

import io.github.student.agile.config.TestConfiguration;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;

import io.github.student.agile.resource.dto.SprintRaportDTO;

import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;

import io.github.student.agile.resource.service.RaportSprintService;
import io.github.student.agile.resource.service.UserService;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static io.github.student.agile.builder.SprintBuilder.createSprintEntity;
import static io.github.student.agile.builder.IssueBuilder.createIssueEntityWithSprint;
import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;

import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_PROGRESS;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
public class RaportSprintServiceTest {

    @Autowired
    private RaportSprintService raportSprintService;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueStatusRepository issueStatusRepository;

    @MockBean
    private UserService userService;

    private Project defaultScrumProject;

    @BeforeEach
    void setUp() {
        Mockito.when(userService.getCurrentLoggedUser())
                .thenReturn(createCurrentLoggedInUser());

        defaultScrumProject =
                projectRepository.save(createProjectEntity());
    }


    @Test
    public void shouldReturnCorrectSprintReport() {
        // given
        Sprint sprint = createTestSprint();

        createTestIssue(sprint, DONE.getStatusID(), 5);
        createTestIssue(sprint, DONE.getStatusID(), 8);
        createTestIssue(sprint, IN_PROGRESS.getStatusID(), 3);

        // when
        SprintRaportDTO raport =
                raportSprintService.getRaport(sprint.getId());

        // then
        assertNotNull(raport);

        assertEquals(sprint.getId(), raport.getSprintId());
        assertEquals(sprint.getName(), raport.getSprintName());
        assertEquals(defaultScrumProject.getId(), raport.getProjectId());
        assertEquals(sprint.getStartDate(), raport.getStartTime());

        assertEquals(3, raport.getCountIssues());
        assertEquals(2, raport.getCountIssuesDone());
        assertEquals(1, raport.getCountIssuesNotDone());

        assertEquals(13, raport.getSumVelocityDone());
        assertEquals(3, raport.getSumVelocityNotDone());
    }


    @Test
    public void shouldThrowExceptionForNonExistingSprint() {
        // given
        Integer sprintId = -10;

        // when
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> raportSprintService.getRaport(sprintId)
        );

        // then
        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
    }


    @Test
    public void shouldReturnZerosForEmptySprint() {
        // given
        Sprint sprint = createTestSprint();

        // when
        SprintRaportDTO raport =
                raportSprintService.getRaport(sprint.getId());

        // then
        assertEquals(0, raport.getCountIssues());
        assertEquals(0, raport.getCountIssuesDone());
        assertEquals(0, raport.getCountIssuesNotDone());

        assertEquals(0, raport.getSumVelocityDone());
        assertEquals(0, raport.getSumVelocityNotDone());
    }


    @Test
    public void shouldCountCompletedAndIncompleteIssues() {
        // given
        Sprint sprint = createTestSprint();

        createTestIssue(sprint, DONE.getStatusID(), 5);
        createTestIssue(sprint, DONE.getStatusID(), 3);
        createTestIssue(sprint, IN_PROGRESS.getStatusID(), 8);

        // when
        SprintRaportDTO raport =
                raportSprintService.getRaport(sprint.getId());

        // then
        assertEquals(3, raport.getCountIssues());
        assertEquals(2, raport.getCountIssuesDone());
        assertEquals(1, raport.getCountIssuesNotDone());
    }


    @Test
    public void shouldReturnCorrectVelocity() {
        // given
        Sprint sprint = createTestSprint();

        createTestIssue(sprint, DONE.getStatusID(), 5);
        createTestIssue(sprint, DONE.getStatusID(), 8);
        createTestIssue(sprint, IN_PROGRESS.getStatusID(), 10);

        // when
        Integer velocity =
                issueRepository.sumVelocityIssuesDoneBySprint_Id(
                        sprint.getId()
                );

        // then
        assertEquals(13, velocity);
    }


    @Test
    public void shouldHandleMissingEstimation() {
        // given
        Sprint sprint = createTestSprint();

        createTestIssue(sprint, DONE.getStatusID(), null);
        createTestIssue(sprint, DONE.getStatusID(), 5);

        // when
        SprintRaportDTO raport =
                raportSprintService.getRaport(sprint.getId());

        // then
        assertEquals(2, raport.getCountIssues());
        assertEquals(2, raport.getCountIssuesDone());
        assertEquals(5, raport.getSumVelocityDone());
    }

    @Test
    public void shouldCalculateVelocityOnlyForSelectedSprint() {
        // given
        Sprint sprint1 = createTestSprint();
        Sprint sprint2 = createTestSprint();

        createTestIssue(sprint1, DONE.getStatusID(), 5);
        createTestIssue(sprint2, DONE.getStatusID(), 13);

        // when
        SprintRaportDTO raport =
                raportSprintService.getRaport(sprint1.getId());

        // then
        assertEquals(sprint1.getId(), raport.getSprintId());
        assertEquals(1, raport.getCountIssues());
        assertEquals(5, raport.getSumVelocityDone());
    }


    private Sprint createTestSprint() {
        return sprintRepository.save(
                createSprintEntity(defaultScrumProject, Boolean.FALSE)
        );
    }

    private Issue createTestIssue(
            Sprint sprint,
            Integer statusId,
            Integer estimation
    ) {
        IssueStatus status =
                issueStatusRepository.getReferenceById(statusId);

        Issue issue = createIssueEntityWithSprint(sprint);

        issue.setIssueStatus(status);
        issue.setEstimation(estimation);

        return issueRepository.saveAndFlush(issue);
    }
}
