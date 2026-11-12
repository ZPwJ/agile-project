package io.github.student.agile.integration;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintProgress;
import io.github.student.agile.resource.dto.SprintDTO;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintProgressRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.SprintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
class SprintLifecycleIntegrationTest {

    @Autowired private SprintService service;
    @Autowired private SprintRepository sprints;
    @Autowired private ProjectRepository projects;
    @Autowired private IssueRepository issues;
    @Autowired private IssueStatusRepository statuses;
    @Autowired private SprintProgressRepository progress;

    private Project project;
    private Sprint sprint;

    @BeforeEach
    void setUp() {
        project = projects.save(Project.builder().name("Lifecycle").build());
        sprint = sprints.save(Sprint.builder().name("Sprint").project(project)
                .active(false).isDone(false).sprintIssues(new HashSet<>()).build());
    }

    @Test
    void preservesStartAndFinalResultBeforeResettingAndDetachingIssues() {
        Issue done = issue(5, 3);
        Issue unfinished = issue(2, null);
        service.startSprint(sprint.getId());
        service.stopSprint(project.getId());

        assertThat(sprints.findById(sprint.getId())).isPresent();
        assertThat(sprint.getIsDone()).isTrue();
        assertThat(sprint.getActive()).isFalse();
        assertThat(sprint.getEndDate()).isAfterOrEqualTo(sprint.getStartDate());
        assertThat(done.getSprint()).isNull();
        assertThat(unfinished.getSprint()).isNull();
        assertThat(unfinished.getIssueStatus().getId()).isEqualTo(1);
        List<SprintProgress> saved = history();
        assertThat(saved).extracting(SprintProgress::getKind)
                .containsExactly(SprintProgress.Kind.START, SprintProgress.Kind.END);
        SprintProgress result = saved.get(1);
        assertThat(result.getIssueCount()).isEqualTo(2);
        assertThat(result.getCompletedIssueCount()).isEqualTo(1);
        assertThat(result.getCompletedPoints()).isEqualTo(3);
        assertThat(result.getTotalPoints()).isEqualTo(3);

        done.setEstimation(99);
        issues.saveAndFlush(done);
        assertThat(history().get(1).getCompletedPoints()).isEqualTo(3);
    }

    @Test
    void protectsClosedSprintAndStartedDate() {
        Issue issue = issue(1, 5);
        service.startSprint(sprint.getId());
        assertThatThrownBy(() -> service.deleteSprint(sprint.getId()))
                .isInstanceOf(IllegalStateException.class);
        SprintDTO edit = SprintDTO.builder().id(sprint.getId()).projectID(project.getId())
                .name("Edited").startTime(sprint.getStartDate().minusDays(1)).build();
        assertThatThrownBy(() -> service.updateSprint(edit)).isInstanceOf(IllegalStateException.class);
        service.stopSprint(project.getId());
        assertThatThrownBy(() -> service.startSprint(sprint.getId())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.deleteSprint(sprint.getId())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.updateSprint(edit)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.addIssueToSprint(issue.getId(), sprint.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsAnotherProjectAndDetachingFromWrongSprint() {
        Project other = projects.save(Project.builder().name("Other").build());
        Issue issue = issues.save(Issue.builder().projectID(other.getId()).build());
        assertThatThrownBy(() -> service.addIssueToSprint(issue.getId(), sprint.getId()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.deleteIssueFromSprint(sprint.getId(), issue.getId()))
                .isInstanceOf(IllegalStateException.class);
        SprintDTO edit = SprintDTO.builder().id(sprint.getId()).projectID(other.getId()).name("Other").build();
        assertThatThrownBy(() -> service.updateSprint(edit)).isInstanceOf(IllegalArgumentException.class);
        assertThat(issue.getSprint()).isNull();
    }

    private Issue issue(int status, Integer estimation) {
        Issue issue = issues.save(Issue.builder().projectID(project.getId()).sprint(sprint)
                .issueStatus(statuses.getReferenceById(status)).estimation(estimation).build());
        sprint.getSprintIssues().add(issue);
        return issue;
    }

    private List<SprintProgress> history() {
        return progress.findAllBySprintIdOrderByCapturedAtAscIdAsc(sprint.getId());
    }
}
