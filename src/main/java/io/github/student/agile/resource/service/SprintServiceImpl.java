package io.github.student.agile.resource.service;

import io.github.student.agile.exception.definition.NullIdentifierException;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintProgress;
import io.github.student.agile.resource.dto.SprintDTO;
import io.github.student.agile.resource.mapper.SprintDTOMapper;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.ACTIVE_SPRINT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.EMPTY_SPRINT;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_ALREADY_HAS_SPRINT;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.NO_ACTIVE_SPRINT_IN_PROJECT;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_ALREADY_STARTED;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_NAME_NOT_UNIQUE;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_START_DATE_IN_THE_FUTURE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.TO_DO;
import static io.github.student.agile.utils.DateUtils.reduceTimeFromZonedDateTime;
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
    private final SprintDTOMapper sprintDTOMapper;
    private final SprintProgressService progressService;

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

        requireOpen(updated);
        if (!Objects.equals(updated.getProject().getId(), project.getId())) {
            throw new IllegalArgumentException("Sprint project cannot be changed");
        }
        if (Boolean.TRUE.equals(updated.getActive())
                && !Objects.equals(updated.getStartDate(), sprintDTO.getStartTime())) {
            throw new IllegalStateException("Started sprint date cannot be changed");
        }

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

        requireOpen(sprintToBeDeleted);
        if (Boolean.TRUE.equals(sprintToBeDeleted.getActive())) {
            throw new IllegalStateException("Started sprint cannot be deleted");
        }

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
        if (!isNull(issue.getSprint())) {
            throw new RuntimeException(ISSUE_ALREADY_HAS_SPRINT);
        }

        requireOpen(sprint);
        if (!Objects.equals(issue.getProjectID(), sprint.getProject().getId())) {
            throw new IllegalArgumentException("Issue and sprint must belong to the same project");
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
        progressService.recordChange(sprint);
    }

    @Override
    public void deleteIssueFromSprint(Integer sprintID, Integer issueID) {

        Sprint sprint = sprintRepository.findById(sprintID)
                .orElseThrow(() -> new RuntimeException(SPRINT_DOES_NOT_EXISTS));
        Issue issue = issueRepository.findById(issueID)
                .orElseThrow(() -> new RuntimeException(ISSUE_DOES_NOT_EXISTS));

        requireOpen(sprint);
        if (issue.getSprint() == null || !Objects.equals(issue.getSprint().getId(), sprintID)) {
            throw new IllegalStateException("Issue does not belong to this sprint");
        }

        Set<Issue> sprintIssues = sprint.getSprintIssues();
        sprintIssues.remove(issue);
        sprint.setSprintIssues(sprintIssues);
        sprintRepository.save(sprint);

        issue.setSprint(null);
        issueRepository.save(issue);
        progressService.recordChange(sprint);
    }

    @Override
    public void startSprint(Integer sprintID) {

        Sprint updated = sprintRepository.findById(ofNullable(sprintID).orElseThrow(NullIdentifierException::new))
                .orElseThrow(() -> new IllegalArgumentException(SPRINT_DOES_NOT_EXISTS));

        requireOpen(updated);
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

        updated.setStartDate(ZonedDateTime.now(ZoneOffset.UTC));

        updated.setActive(TRUE);
        sprintRepository.save(updated);
        progressService.record(updated, SprintProgress.Kind.START);
    }

    @Override
    public void stopSprint(Integer projectID) {

        Project project = projectRepository.findById(projectID)
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));

        Sprint updated = sprintRepository.findByActiveAndProject(TRUE, project)
                .orElseThrow(() -> new RuntimeException(NO_ACTIVE_SPRINT_IN_PROJECT));

        updated.setEndDate(ZonedDateTime.now(ZoneOffset.UTC));
        // Capture the outcome before unfinished issues are reset and detached.
        progressService.record(updated, SprintProgress.Kind.END);
        List<Issue> issues = issueRepository.findAllBySprint(updated);
        for (Issue issue : issues) {
            if (issue.getIssueStatus() == null
                    || !DONE.getStatusID().equals(issue.getIssueStatus().getId())) {
                issue.setIssueStatus(issueStatusRepository.findById(TO_DO.getStatusID()).orElseThrow());
            }
            issue.setSprint(null);
        }
        issueRepository.saveAll(issues);
        updated.setSprintIssues(new HashSet<>());
        updated.setActive(FALSE);
        updated.setIsDone(TRUE);
        sprintRepository.save(updated);
    }

    private void requireOpen(Sprint sprint) {
        if (Boolean.TRUE.equals(sprint.getIsDone())) {
            throw new IllegalStateException("Closed sprint cannot be changed or restarted");
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
