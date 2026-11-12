package io.github.student.agile.integration;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Backlog;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintProgress;
import io.github.student.agile.resource.dto.request.IssueMRO;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintProgressRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.IssueService;
import io.github.student.agile.resource.service.SprintService;
import io.github.student.agile.resource.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
class SprintProgressIntegrationTest {

    @Autowired private SprintService sprintService;
    @Autowired private IssueService issueService;
    @Autowired private SprintRepository sprints;
    @Autowired private ProjectRepository projects;
    @Autowired private IssueRepository issues;
    @Autowired private IssueStatusRepository statuses;
    @Autowired private SprintProgressRepository progress;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockBean private UserService userService;

    private Project project;
    private Sprint sprint;

    @BeforeEach
    void setUp() {
        Backlog backlog = new Backlog();
        backlog.setIssues(new HashSet<>());
        project = projects.save(Project.builder().name("History").backlog(backlog).build());
        sprint = sprint("First");
    }

    @Test
    void capturesScopeEstimationCompletionRemovalAndDeletion() {
        Issue first = issue(null);
        sprintService.addIssueToSprint(first.getId(), sprint.getId());
        assertThat(history(sprint)).isEmpty();
        sprintService.startSprint(sprint.getId());
        assertState(sprint, 1, 0, 0, 0);

        Issue added = issue(5);
        sprintService.addIssueToSprint(added.getId(), sprint.getId());
        assertState(sprint, 2, 0, 5, 0);
        issueService.updateIssue(IssueMRO.builder().issueID(first.getId())
                .name("Estimated").description("Work").estimation(3).build());
        assertState(sprint, 2, 0, 8, 0);
        for (int status = 2; status <= 5; status++) {
            issueService.changeIssueStatus(first.getId(), status);
        }
        assertState(sprint, 2, 1, 8, 3);
        sprintService.deleteIssueFromSprint(sprint.getId(), added.getId());
        assertState(sprint, 1, 1, 3, 3);
        issueService.deleteIssue(first.getId());
        assertState(sprint, 0, 0, 0, 0);
        sprintService.stopSprint(project.getId());
        assertState(sprint, 0, 0, 0, 0);

        entityManager.flush();
        entityManager.clear();
        List<SprintProgress> saved = history(sprint);
        assertThat(saved).hasSize(10);
        assertThat(saved.getFirst().getKind()).isEqualTo(SprintProgress.Kind.START);
        assertThat(saved.getLast().getKind()).isEqualTo(SprintProgress.Kind.END);
        assertThat(saved).extracting(SprintProgress::getCapturedAt).isSorted();
        assertThat(saved.get(3).getTotalPoints()).isEqualTo(8);
    }

    @Test
    void laterSprintAndIssueEditsDoNotRewriteEarlierSprint() {
        Issue carried = issue(5);
        sprintService.addIssueToSprint(carried.getId(), sprint.getId());
        sprintService.startSprint(sprint.getId());
        sprintService.stopSprint(project.getId());
        List<Long> firstIDs = history(sprint).stream().map(SprintProgress::getId).toList();

        Sprint next = sprint("Second");
        sprintService.addIssueToSprint(carried.getId(), next.getId());
        sprintService.startSprint(next.getId());
        issueService.updateIssue(IssueMRO.builder().issueID(carried.getId())
                .name("Reestimated").description("Work").estimation(8).build());
        assertState(next, 1, 0, 8, 0);
        sprintService.stopSprint(project.getId());
        assertState(sprint, 1, 0, 5, 0);
        assertThat(history(sprint)).extracting(SprintProgress::getId).containsExactlyElementsOf(firstIDs);
        assertThat(history(next)).hasSize(3);
    }

    @Test
    void legacyActiveSprintHasOnlyObservedPointsAndNoInventedStart() {
        sprint.setActive(true);
        Issue old = issue(-3);
        old.setSprint(sprint);
        sprint.getSprintIssues().add(old);
        issues.save(old);
        assertThat(history(sprint)).isEmpty();
        sprintService.stopSprint(project.getId());
        assertThat(history(sprint)).extracting(SprintProgress::getKind)
                .containsExactly(SprintProgress.Kind.END);
        assertState(sprint, 1, 0, 0, 0);
    }

    @Test
    void rollsBackProgressTogetherWithTheChangedData() {
        long previousCount = progress.count();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            Project temporaryProject = projects.save(Project.builder().name("Rollback").build());
            Sprint temporarySprint = sprints.save(Sprint.builder().name("Rollback")
                    .project(temporaryProject).active(false).isDone(false)
                    .sprintIssues(new HashSet<>()).build());
            Issue temporaryIssue = issues.save(Issue.builder().projectID(temporaryProject.getId())
                    .estimation(5).sprint(temporarySprint).build());
            temporarySprint.getSprintIssues().add(temporaryIssue);
            sprintService.startSprint(temporarySprint.getId());
            entityManager.flush();
            assertThat(history(temporarySprint)).hasSize(1);
            throw new IllegalStateException("Rollback operation");
        })).isInstanceOf(IllegalStateException.class).hasMessage("Rollback operation");
        assertThat(progress.count()).isEqualTo(previousCount);
    }

    private Sprint sprint(String name) {
        return sprints.save(Sprint.builder().name(name).project(project).active(false).isDone(false)
                .sprintIssues(new HashSet<>()).build());
    }

    private Issue issue(Integer estimation) {
        Issue issue = issues.save(Issue.builder().name("Work").description("Work")
                .projectID(project.getId()).issueStatus(statuses.getReferenceById(1))
                .estimation(estimation).build());
        project.getBacklog().getIssues().add(issue);
        return issue;
    }

    private List<SprintProgress> history(Sprint target) {
        return progress.findAllBySprintIdOrderByCapturedAtAscIdAsc(target.getId());
    }

    private void assertState(Sprint target, int count, int done, long total, long completed) {
        SprintProgress point = history(target).getLast();
        assertThat(point.getIssueCount()).isEqualTo(count);
        assertThat(point.getCompletedIssueCount()).isEqualTo(done);
        assertThat(point.getTotalPoints()).isEqualTo(total);
        assertThat(point.getCompletedPoints()).isEqualTo(completed);
    }
}
