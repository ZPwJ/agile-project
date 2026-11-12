package io.github.student.agile.resource.service;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintProgress;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.SprintProgressRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;

@Service
@Transactional
@RequiredArgsConstructor
public class SprintProgressService {

    private final IssueRepository issueRepository;
    private final SprintProgressRepository progressRepository;

    public void record(Sprint sprint, SprintProgress.Kind kind) {
        List<Issue> issues = issueRepository.findAllBySprint(sprint);
        List<Issue> completed = issues.stream()
                .filter(issue -> issue.getIssueStatus() != null
                        && DONE.getStatusID().equals(issue.getIssueStatus().getId()))
                .toList();
        ZonedDateTime timestamp = switch (kind) {
            case START -> sprint.getStartDate();
            case END -> sprint.getEndDate();
            case CHANGE -> ZonedDateTime.now(ZoneOffset.UTC);
        };
        progressRepository.save(SprintProgress.builder()
                .sprint(sprint)
                .capturedAt(timestamp)
                .kind(kind)
                .issueCount(issues.size())
                .completedIssueCount(completed.size())
                .totalPoints(issues.stream().mapToLong(this::points).sum())
                .completedPoints(completed.stream().mapToLong(this::points).sum())
                .build());
    }

    private long points(Issue issue) {
        return issue.getEstimation() == null ? 0 : Math.max(0, issue.getEstimation());
    }
}
