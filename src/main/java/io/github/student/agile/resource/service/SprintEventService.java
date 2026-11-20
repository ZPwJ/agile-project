package io.github.student.agile.resource.service;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.Sprint;

import java.time.ZonedDateTime;

public interface SprintEventService {

    void recordSprintStarted(Sprint sprint, ZonedDateTime occurredAt);

    void recordIssueAddedToSprint(Sprint sprint, Issue issue);

    void recordIssueRemovedFromSprint(Sprint sprint, Issue issue);

    void recordStatusChanged(Issue issue, IssueStatus previousStatus, IssueStatus newStatus);

    void recordEstimationChanged(Issue issue, Integer previousEstimation, Integer newEstimation);

    void recordSprintStopped(Sprint sprint, ZonedDateTime occurredAt);

    void deleteEventsForSprint(Integer sprintId);
}
