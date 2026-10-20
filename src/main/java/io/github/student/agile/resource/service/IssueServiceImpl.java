package io.github.student.agile.resource.service;

import io.github.student.agile.exception.definition.NullIdentifierException;
import io.github.student.agile.resource.domain.Backlog;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssuePriority;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.IssueType;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.dto.IssueDTO;
import io.github.student.agile.resource.dto.IssueGeneralDTO;
import io.github.student.agile.resource.dto.request.IssueMRO;
import io.github.student.agile.resource.mapper.IssueDTOMapper;
import io.github.student.agile.resource.mapper.IssueGeneralDTOMapper;
import io.github.student.agile.resource.repository.BacklogRepository;
import io.github.student.agile.resource.repository.IssuePriorityRepository;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.IssueTypeRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.IMPOSSIBLE_ISSUE_STATUS_CHANGE_SPRINT_EMPTY;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.IMPOSSIBLE_ISSUE_STATUS_CHANGE_SPRINT_NOT_ACTIVE;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_PRIORITY_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_TYPE_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.NULL_IDENTIFIER;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_ID_EMPTY;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.STATUS_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.STATUS_FLOW_ORDER_VIOLATION;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.CODE_REVIEW;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_PROGRESS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_TESTS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.TO_DO;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueServiceImpl implements IssueService{

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final IssueStatusRepository issueStatusRepository;
    private final IssuePriorityRepository issuePriorityRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final SprintRepository sprintRepository;
    private final BacklogRepository backlogRepository;
    private final UserService userService;
    private final IssueDTOMapper issueDTOMapper;
    private final IssueGeneralDTOMapper issueGeneralDTOMapper;

    @Override
    public IssueDTO createIssue(IssueMRO issueDTO) {

        Project project = projectRepository.findById(issueDTO.getProjectID())
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));
        IssueStatus status = issueStatusRepository.findById(TO_DO.getStatusID())
                .orElseThrow(() -> new RuntimeException(STATUS_DOES_NOT_EXISTS));

        Optional<Integer> issueType = ofNullable(issueDTO.getIssueTypeID());
        Optional<Integer> issuePriority = ofNullable(issueDTO.getIssuePriorityID());
        IssueType type = null;
        IssuePriority priority = null;

        if (issueType.isPresent()) {
            type = issueTypeRepository.findById(issueDTO.getIssueTypeID())
                    .orElseThrow(() -> new RuntimeException(ISSUE_TYPE_DOES_NOT_EXISTS));
        }

        if (issuePriority.isPresent()) {
            priority = issuePriorityRepository.findById(issueDTO.getIssuePriorityID())
                    .orElseThrow(() -> new RuntimeException(ISSUE_PRIORITY_DOES_NOT_EXISTS));
        }

        Issue issue = Issue.builder()
                .name(issueDTO.getName())
                .description(issueDTO.getDescription())
                .estimation(issueDTO.getEstimation())
                .projectID(project.getId())
                .reporterID(userService.getCurrentLoggedUser().getUuid())
                .issueStatus(isNull(status) ? null : status)
                .issueType(isNull(type) ? null : type)
                .issuePriority(isNull(priority) ? null : priority)
                .build();
        issueRepository.save(issue);

        Backlog backlog = project.getBacklog();
        Set<Issue> backlogIssues = backlog.getIssues();
        backlogIssues.add(issue);
        backlogRepository.save(backlog);

        return issueDTOMapper.map(issue);
    }

    @Override
    public IssueDTO updateIssue(IssueMRO issueDTO) {

        Issue updated = issueRepository.findById(
                ofNullable(issueDTO.getIssueID()).orElseThrow(
                        () -> new IllegalArgumentException(NULL_IDENTIFIER))
                )
                .orElseThrow(() -> new RuntimeException(ISSUE_DOES_NOT_EXISTS));

        Optional<Integer> issuePriority = ofNullable(issueDTO.getIssuePriorityID());
        IssuePriority priority = null;

        if (issuePriority.isPresent()) {
            priority = issuePriorityRepository.findById(issueDTO.getIssuePriorityID())
                    .orElseThrow(() -> new RuntimeException(ISSUE_PRIORITY_DOES_NOT_EXISTS));
        }

        updated.setName(ofNullable(issueDTO.getName()).orElseThrow());
        updated.setDescription(ofNullable(issueDTO.getDescription()).orElseThrow());
        updated.setEstimation(issueDTO.getEstimation());
        updated.setIssuePriority(isNull(priority) ? null : priority);
        issueRepository.save(updated);
        return issueDTOMapper.map(updated);
    }

    @Override
    public void deleteIssue(Integer issueID) {

        Issue issue = issueRepository.findById(issueID)
                .orElseThrow(() -> new RuntimeException(ISSUE_DOES_NOT_EXISTS));

        Integer projectID = issue.getProjectID();
        if (isNull(projectID)) {
            throw new RuntimeException(PROJECT_ID_EMPTY);
        }

        Project project = projectRepository.findById(projectID)
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));


        Backlog backlog = project.getBacklog();

        Set<Issue> backlogIssues = backlog.getIssues();
        backlogIssues.remove(issue);
        backlog.setIssues(backlogIssues);
        backlogRepository.save(backlog);
        issueRepository.deleteById(issueID);
    }

    @Override
    public IssueDTO getIssueDetails(Integer issueID) {

        Issue issue = issueRepository.findById(issueID)
                .orElseThrow(() -> new RuntimeException(ISSUE_DOES_NOT_EXISTS));
        return issueDTOMapper.map(issue);
    }

    @Override
    public List<IssueGeneralDTO> getIssuesGeneral(Integer projectID) {

        Project project = projectRepository.findById(projectID)
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));
        return project.getBacklog()
                .getIssues()
                .stream()
                .map(issueGeneralDTOMapper::map)
                .collect(Collectors.toList());
    }

    @Override
    public List<IssueGeneralDTO> getProductBacklog(Integer projectID) {
        Project project = projectRepository.findById(projectID)
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));
        return project.getBacklog()
                .getIssues()
                .stream()
                .filter(issue -> isNull(issue.getSprint()))
                .filter(issue -> issue.getIssueStatus().getId().equals(TO_DO.getStatusID()))
                .map(issueGeneralDTOMapper::map)
                .collect(Collectors.toList());
    }

    @Override
    public List<IssueGeneralDTO> getBacklogHistory(Integer projectID) {

        Project project = projectRepository.findById(projectID)
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));

        return project.getBacklog()
                .getIssues()
                .stream()
                .filter(issue -> DONE.getStatusID().equals(issue.getIssueType().getId()))
                .map(issueGeneralDTOMapper::map)
                .collect(Collectors.toList());
    }

    @Override
    public List<IssueGeneralDTO> getGeneralIssuesInfoByUserID() {
        return issueRepository
                .findAllByAssigneeID(userService.getCurrentLoggedUser().getUuid())
                .stream()
                .map(issueGeneralDTOMapper::map)
                .collect(Collectors.toList());
    }

    @Override
    public List<IssueGeneralDTO> getGeneralIssuesInfoBySprintID(Integer sprintID) {

        Sprint sprint = sprintRepository.findById(ofNullable(sprintID).orElseThrow(NullIdentifierException::new))
                .orElseThrow(() -> new IllegalArgumentException(SPRINT_DOES_NOT_EXISTS));

        return issueRepository.findAllBySprint(sprint)
                .stream()
                .map(issueGeneralDTOMapper::map)
                .collect(Collectors.toList());
    }

    @Override
    public void assignCurrentUserToIssue(Integer issueID) {

        Issue updated = issueRepository.findById(issueID)
                .orElseThrow(() -> new RuntimeException(ISSUE_DOES_NOT_EXISTS));
        updated.setAssigneeID(userService.getCurrentLoggedUser().getUuid());
        issueRepository.save(updated);
    }

    @Override
    public void deleteUserToIssueAssignment(Integer issueID) {

        Issue updated = issueRepository.findById(issueID)
                .orElseThrow(() -> new RuntimeException(ISSUE_DOES_NOT_EXISTS));
        updated.setAssigneeID(null);
        issueRepository.save(updated);
    }

    @Override
    public void changeIssueStatus(Integer issueID, Integer statusID) {

        Issue updated = issueRepository
                .findById(ofNullable(issueID).orElseThrow(NullIdentifierException::new))
                .orElseThrow(() -> new IllegalArgumentException(ISSUE_DOES_NOT_EXISTS));

        Optional<Sprint> issueSprint = ofNullable(updated.getSprint());
        if (issueSprint.isEmpty()) {
            throw new RuntimeException(IMPOSSIBLE_ISSUE_STATUS_CHANGE_SPRINT_EMPTY);
        }

        if (!issueSprint.get().getActive()) {
            throw new RuntimeException(IMPOSSIBLE_ISSUE_STATUS_CHANGE_SPRINT_NOT_ACTIVE);
        }

        IssueStatus currentStatus = updated.getIssueStatus();
        IssueStatus newStatus = issueStatusRepository.findById(ofNullable(statusID).orElseThrow(NullIdentifierException::new))
                .orElseThrow(() -> new IllegalArgumentException(STATUS_DOES_NOT_EXISTS));

        if (!isStatusChangeFlowCorrect(currentStatus, newStatus)) {
            throw new RuntimeException(STATUS_FLOW_ORDER_VIOLATION);
        }
        updated.setIssueStatus(newStatus);
        issueRepository.save(updated);
    }

    private boolean isStatusChangeFlowCorrect(IssueStatus current, IssueStatus changeRequested) {

        boolean isIssueDone = DONE.getStatusID().equals(current.getId());
        boolean isNaturalFlow = current.getId().equals(changeRequested.getId()-1);
        boolean isBackFromCodeReviewToInProgress = CODE_REVIEW.getStatusID().equals(current.getId()) && IN_PROGRESS.getStatusID().equals(changeRequested.getId());
        boolean isBackFromTestsToInProgress = IN_TESTS.getStatusID().equals(current.getId()) && IN_PROGRESS.getStatusID().equals(changeRequested.getId());

        if (isIssueDone) {
            return false;
        }
        else return isNaturalFlow || isBackFromCodeReviewToInProgress || isBackFromTestsToInProgress;
    }
}
