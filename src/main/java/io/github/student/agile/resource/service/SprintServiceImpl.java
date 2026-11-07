package io.github.student.agile.resource.service;

import io.github.student.agile.exception.definition.NullIdentifierException;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.dto.SprintDTO;
import io.github.student.agile.resource.mapper.SprintDTOMapper;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.ACTIVE_SPRINT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.EMPTY_SPRINT;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_ALREADY_DONE;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_ALREADY_HAS_SPRINT;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.NO_ACTIVE_SPRINT_IN_PROJECT;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_ALREADY_FINISHED;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_ALREADY_STARTED;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_NAME_NOT_UNIQUE;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_START_DATE_IN_THE_FUTURE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.TO_DO;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.ADDED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.REMOVED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.SCOPE_AT_START;
import static io.github.student.agile.utils.DateUtils.reduceTimeFromZonedDateTime;
import static io.github.student.agile.utils.collection.CollectionsUtils.splitSetIntoTwoSubSets;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

@Service
@Transactional
@RequiredArgsConstructor
public class SprintServiceImpl implements SprintService {

    private static final String DATES_VALID = "Dates are valid";

    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final IssueStatusRepository issueStatusRepository;
    private final SprintIssueEventRepository sprintIssueEventRepository;
    private final SprintDTOMapper sprintDTOMapper;

    @Override
    public SprintDTO getActiveSprint(Integer projectID) {

        Project project = projectRepository.findById(projectID)
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));

        if (sprintRepository.findByActiveAndProject(TRUE, project).isPresent()) {
            return sprintDTOMapper.map(sprintRepository.findByActiveAndProject(TRUE, project).get());
        }
        return null;
    }

    @Override
    public List<SprintDTO> getAllNonActivatedSprints(Integer projectID) {

        Project project = projectRepository.findById(projectID)
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));

        if (sprintRepository.findAllByActiveAndIsDoneAndProject(FALSE, FALSE, project).isEmpty()) {
            return new ArrayList<>();
        }

        return sprintRepository.findAllByActiveAndIsDoneAndProject(FALSE, FALSE, project)
                .get()
                .stream()
                .map(sprintDTOMapper::map)
                .collect(Collectors.toList());
    }

    @Override
    public SprintDTO createSprint(SprintDTO sprintDTO) {

        Project project = projectRepository.findById(sprintDTO.getProjectID())
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));

        if (sprintRepository.existsByNameAndProject(sprintDTO.getName(), project)) {
            throw new RuntimeException(SPRINT_NAME_NOT_UNIQUE);
        }

        Sprint sprint = Sprint.builder()
                .name(sprintDTO.getName())
                .startDate(sprintDTO.getStartTime())
                .sprintIssues(new HashSet<>())
                .sprintGoal(sprintDTO.getSprintGoal())
                .active(FALSE)
                .isDone(FALSE)
                .project(project)
                .build();
        sprintRepository.save(sprint);
        return sprintDTOMapper.map(sprint);
    }

    @Override
    public SprintDTO updateSprint(SprintDTO sprintDTO) {

        Project project = projectRepository.findById(sprintDTO.getProjectID())
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));

        Sprint updated = sprintRepository.findById(sprintDTO.getId())
                .orElseThrow(() -> new RuntimeException(SPRINT_DOES_NOT_EXISTS));

        if (!updated.getName().equals(sprintDTO.getName())) {
            if (sprintRepository.existsByNameAndProject(sprintDTO.getName(), project)) {
                throw new RuntimeException(SPRINT_NAME_NOT_UNIQUE);
            }
        }

        updated.setName(sprintDTO.getName());
        updated.setSprintGoal(sprintDTO.getSprintGoal());
        updated.setStartDate(sprintDTO.getStartTime());

        sprintRepository.save(updated);
        return sprintDTOMapper.map(updated);
    }

    @Override
    public void deleteSprint(Integer sprintID) {

        Sprint sprintToBeDeleted = sprintRepository.findById(sprintID)
                .orElseThrow(() -> new RuntimeException(SPRINT_DOES_NOT_EXISTS));
        rejectIfFinished(sprintToBeDeleted);

        if (!sprintToBeDeleted.getSprintIssues().isEmpty()) {
            List<Issue> issues = issueRepository.findAllBySprint(sprintToBeDeleted)
                    .stream()
                    .peek(sprint -> sprint.setSprint(null))
                    .toList();
            issueRepository.saveAll(issues);
        }

        sprintToBeDeleted.setSprintIssues(new HashSet<>());
        sprintRepository.save(sprintToBeDeleted);

        sprintRepository.deleteById(sprintID);
    }

    @Override
    public void addIssueToSprint(Integer issueID, Integer sprintID) {

        Issue issue = issueRepository.findById(issueID)
                .orElseThrow(() -> new RuntimeException(ISSUE_DOES_NOT_EXISTS));
        Sprint sprint = sprintRepository.findById(sprintID)
                .orElseThrow(() -> new RuntimeException(SPRINT_DOES_NOT_EXISTS));
        rejectIfFinished(sprint);
        if (!isNull(issue.getSprint())) {
            throw new RuntimeException(ISSUE_ALREADY_HAS_SPRINT);
        }
        // DONE is terminal, so a done issue could never be completed again and would stay open in reports.
        if (!isNull(issue.getIssueStatus()) && DONE.getStatusID().equals(issue.getIssueStatus().getId())) {
            throw new RuntimeException(ISSUE_ALREADY_DONE);
        }

        Set<Issue> sprintIssues;
        if (isNull(sprint.getSprintIssues())) {
            sprintIssues = new HashSet<>(List.of(issue));
        } else {
            sprintIssues = sprint.getSprintIssues();
            sprintIssues.add(issue);
        }
        sprint.setSprintIssues(sprintIssues);
        sprintRepository.save(sprint);

        issue.setSprint(sprint);
        issueRepository.save(issue);

        if (TRUE.equals(sprint.getActive())) {
            sprintIssueEventRepository.save(SprintIssueEvent.of(sprint, issue, ADDED));
        }
    }

    @Override
    public void deleteIssueFromSprint(Integer sprintID, Integer issueID) {

        Sprint sprint = sprintRepository.findById(sprintID)
                .orElseThrow(() -> new RuntimeException(SPRINT_DOES_NOT_EXISTS));
        Issue issue = issueRepository.findById(issueID)
                .orElseThrow(() -> new RuntimeException(ISSUE_DOES_NOT_EXISTS));

        // The issue's own sprint is logged, because the endpoint does not check that it matches sprintID.
        Sprint currentSprint = issue.getSprint();
        if (!isNull(currentSprint) && TRUE.equals(currentSprint.getActive())) {
            sprintIssueEventRepository.save(SprintIssueEvent.of(currentSprint, issue, REMOVED));
        }

        Set<Issue> sprintIssues = sprint.getSprintIssues();
        sprintIssues.remove(issue);
        sprint.setSprintIssues(sprintIssues);
        sprintRepository.save(sprint);

        issue.setSprint(null);
        issueRepository.save(issue);
    }

    @Override
    public void startSprint(Integer sprintID) {

        Sprint updated = sprintRepository.findById(ofNullable(sprintID).orElseThrow(NullIdentifierException::new))
                .orElseThrow(() -> new IllegalArgumentException(SPRINT_DOES_NOT_EXISTS));
        rejectIfFinished(updated);

        if(sprintRepository.findByActiveAndProject(TRUE, updated.getProject()).isPresent()) {
            throw new RuntimeException(ACTIVE_SPRINT_EXISTS);
        }

        if (updated.getSprintIssues().isEmpty()) {
            throw new RuntimeException(EMPTY_SPRINT);
        }

        Pair<Boolean, String> validationResult = validateSprintDatesForStart(updated);
        if (!validationResult.getKey()) {
            throw new RuntimeException(validationResult.getValue());
        }

        if (isNull(updated.getStartDate())) {
            updated.setStartDate(ZonedDateTime.now());
        }

        updated.setActive(TRUE);
        updated.setStartedAt(ZonedDateTime.now());
        sprintRepository.save(updated);

        sprintIssueEventRepository.saveAll(updated.getSprintIssues()
                .stream()
                .map(issue -> SprintIssueEvent.of(updated, issue, SCOPE_AT_START))
                .toList());
    }

    @Override
    public void stopSprint(Integer projectID) {

        Project project = projectRepository.findById(projectID)
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));

        Sprint updated = sprintRepository.findByActiveAndProject(TRUE, project)
                .orElseThrow(() -> new RuntimeException(NO_ACTIVE_SPRINT_IN_PROJECT));

        Pair<Set<Issue>, Set<Issue>> dividedSet = splitSetIntoTwoSubSets(updated.getSprintIssues(), issue -> issue.getIssueStatus().getId().equals(DONE.getStatusID()));
        Set<Issue> doneIssues = dividedSet.getKey();
        Set<Issue> uncompletedIssues = dividedSet.getValue();

        doneIssues.forEach(issue -> {
            issue.setSprint(null);
            issueRepository.save(issue);
        });

        uncompletedIssues.forEach(issue -> {
            issue.setSprint(null);
            issue.setIssueStatus(issueStatusRepository.findById(TO_DO.getStatusID()).orElseThrow());
            issueRepository.save(issue);
        });

        updated.setSprintIssues(new HashSet<>());
        sprintRepository.save(updated);

        updated.setActive(FALSE);
        updated.setIsDone(TRUE);
        updated.setEndedAt(ZonedDateTime.now());
        sprintRepository.save(updated);
    }

    private void rejectIfFinished(Sprint sprint) {
        if (TRUE.equals(sprint.getIsDone())) {
            throw new RuntimeException(SPRINT_ALREADY_FINISHED);
        }
    }

    private Pair<Boolean, String> validateSprintDatesForStart(Sprint sprint) {

        ZonedDateTime currentSystemDate = reduceTimeFromZonedDateTime(ZonedDateTime.now());
        Optional<ZonedDateTime> startSprintDate = ofNullable(sprint.getStartDate());
        boolean isSprintActive = sprint.getActive();

        if (isSprintActive) {
            return Pair.of(FALSE, SPRINT_ALREADY_STARTED);
        }

        if (startSprintDate.isEmpty()) {
            return Pair.of(TRUE, DATES_VALID);
        }

        ZonedDateTime startSprintDateReduced = reduceTimeFromZonedDateTime(startSprintDate.get());
        boolean isLaterThanCurrentSystemDate = startSprintDateReduced.isAfter(currentSystemDate);
        if (isLaterThanCurrentSystemDate) {
            return Pair.of(FALSE, SPRINT_START_DATE_IN_THE_FUTURE);
        }

        return Pair.of(TRUE, DATES_VALID);
    }
}
