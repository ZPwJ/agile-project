
package io.github.student.agile.resource.demo;

import io.github.student.agile.resource.domain.*;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.repository.*;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Component
@Profile("dev")
public class SprintAnalyticsDemoData implements CommandLineRunner {

    private static final ZoneId WARSAW =
            ZoneId.of("Europe/Warsaw");

    private final ProjectRepository projects;
    private final BacklogRepository backlogs;
    private final SprintRepository sprints;
    private final IssueRepository issues;
    private final IssueStatusRepository statuses;
    private final SprintIssueSnapshotRepository snapshots;
    private final IssueStatusHistoryRepository histories;

    public SprintAnalyticsDemoData(
            ProjectRepository projects,
            BacklogRepository backlogs,
            SprintRepository sprints,
            IssueRepository issues,
            IssueStatusRepository statuses,
            SprintIssueSnapshotRepository snapshots,
            IssueStatusHistoryRepository histories) {

        this.projects = projects;
        this.backlogs = backlogs;
        this.sprints = sprints;
        this.issues = issues;
        this.statuses = statuses;
        this.snapshots = snapshots;
        this.histories = histories;
    }

    
@Override
public void run(String... args) {

    createProject("Demo Alpha", 1);
    createProject("Demo Beta", 2);
}





    private void createProject(String name, int number) {
        if (projects.findAll().stream()
                .anyMatch(p -> name.equals(p.getName()))) {
            return;
        }


Backlog backlog = new Backlog();

Project project = Project.builder()
        .name(name)
        .description("Sprint analytics demo")
        .projectOwner("demo")
        .backlog(backlog)
        .build();

backlog.setProject(project);

project = projects.save(project);


        for (int i = 1; i <= 3; i++) {
            ZonedDateTime start = ZonedDateTime.of(
                    2026, number + i, 1,
                    9, 0, 0, 0, WARSAW
            );

            ZonedDateTime end = start.plusDays(13);

            boolean finished = i < 3;

            Sprint sprint = Sprint.builder()
                    .name(name + " Sprint " + i)
                    .project(project)
                    .startDate(start)
                    .endDate(finished ? end : null)
                    .active(!finished)
                    .isDone(finished)
                    .build();

            sprint = sprints.save(sprint);

            createIssue(sprint, "Done task", 5, true, finished, start);
            createIssue(sprint, "Open task", 3, false, finished, start);
            createIssue(sprint, "Unestimated task", null, false, finished, start);
        }
    }

    private void createIssue(
            Sprint sprint,
            String name,
            Integer estimation,
            boolean completed,
            boolean finished,
            ZonedDateTime start) {

        IssueStatus done = statuses.findById(
                IssueStatusID.DONE.getStatusID()
        ).orElseThrow();

        IssueStatus todo = statuses.findById(
                IssueStatusID.TO_DO.getStatusID()
        ).orElseThrow();

        Issue issue = Issue.builder()
                .name(name)
                .projectID(sprint.getProject().getId())
                .estimation(estimation)
                .sprint(finished ? null : sprint)
                .issueStatus(completed ? done : todo)
                .build();

        issue = issues.save(issue);

        if (finished) {
            snapshots.save(
                    SprintIssueSnapshot.builder()
                            .sprint(sprint)
                            .issueId(issue.getId())
                            .estimation(estimation)
                            .finalStatus(completed ? done : todo)
                            .completed(completed)
                            .build()
            );
        }

        histories.save(
                IssueStatusHistory.builder()
                        .issueId(issue.getId())
                        .sprintId(sprint.getId())
                        .oldStatus(todo)
                        .newStatus(completed ? done : todo)
                        .changedAt(
                                start.plusDays(completed ? 5 : 1)
                                        .toInstant()
                        )
                        .build()
        );
    }
}

