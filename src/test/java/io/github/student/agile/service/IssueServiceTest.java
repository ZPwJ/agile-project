package io.github.student.agile.service;

import io.github.student.agile.builder.IssueBuilder;
import io.github.student.agile.builder.SprintBuilder;
import io.github.student.agile.builder.UserBuilder;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Backlog;
import io.github.student.agile.resource.domain.Comment;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.dictionary.IssuePriorityID;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.dto.CommentDTO;
import io.github.student.agile.resource.dto.IssueDTO;
import io.github.student.agile.resource.dto.IssueGeneralDTO;
import io.github.student.agile.resource.dto.request.IssueMRO;
import io.github.student.agile.resource.repository.BacklogRepository;
import io.github.student.agile.resource.repository.CommentRepository;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.IssueService;
import io.github.student.agile.resource.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.github.student.agile.builder.CommentBuilder.DEFAULT_COMMENT_BODY;
import static io.github.student.agile.builder.IssueBuilder.OTHER_ISSUE_DESCRIPTION;
import static io.github.student.agile.builder.IssueBuilder.OTHER_ISSUE_ESTIMATION;
import static io.github.student.agile.builder.IssueBuilder.OTHER_ISSUE_NAME;
import static io.github.student.agile.builder.IssueBuilder.createDefaultIssue;
import static io.github.student.agile.builder.IssueBuilder.createIssueEntity;
import static io.github.student.agile.builder.IssueBuilder.createIssueEntityWithAssignee;
import static io.github.student.agile.builder.IssueBuilder.createIssueEntityWithIssueStatus;
import static io.github.student.agile.builder.IssueBuilder.createIssueEntityWithSprint;
import static io.github.student.agile.builder.IssueBuilder.createIssueWithFakeIssuePriorityID;
import static io.github.student.agile.builder.IssueBuilder.createIssueWithFakeIssueTypeID;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static io.github.student.agile.builder.UserBuilder.CURRENT_USER_UUID;
import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.IMPOSSIBLE_ISSUE_STATUS_CHANGE_SPRINT_EMPTY;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.IMPOSSIBLE_ISSUE_STATUS_CHANGE_SPRINT_NOT_ACTIVE;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_PRIORITY_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.ISSUE_TYPE_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.NULL_IDENTIFIER;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.STATUS_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.STATUS_FLOW_ORDER_VIOLATION;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
public class IssueServiceTest {

    @Autowired
    private IssueStatusRepository issueStatusRepository;

    @Autowired
    private BacklogRepository backlogRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueService issueService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private CommentRepository commentRepository;

    @MockBean
    private UserService userService;

    private Project defaultScrumProject;

    @BeforeEach
    public void setUp() {

        // global default project configuration
        defaultScrumProject = projectRepository.save(createProjectEntity());
        Backlog backlog = defaultScrumProject.getBacklog();
        backlog.setProject(defaultScrumProject);
        backlogRepository.save(backlog);

        // mocks
        when(userService.getCurrentLoggedUser()).thenReturn(createCurrentLoggedInUser());
    }

    @Test
    public void getIssuesGeneral_happyPathTest() {

        //given
        int issuesAmount = 10;
        List<IssueMRO> issueMROList = IssueBuilder.createManyIssues(issuesAmount, defaultScrumProject.getId());
        long expectedIssueAmount = issueRepository.count() + issuesAmount;
        Integer projectID = defaultScrumProject.getId();
        issueMROList.forEach(issue -> issueService.createIssue(issue));

        //when
        long actualIssueAmount = issueRepository.count();
        List<IssueGeneralDTO> actualReturnList = issueService.getIssuesGeneral(projectID);

        //then
        assertEquals(expectedIssueAmount, actualIssueAmount);
        assertEquals(issuesAmount, actualReturnList.size());
        actualReturnList.forEach(issue -> assertEquals(IssueStatusID.TO_DO.getStatusID(), issue.getIssueStatusID()));
    }

    @Test
    public void getIssuesGeneral_throwsProjectDoesNotExistsExceptionTest() {

        //given
        Integer fakeProjectID = defaultScrumProject.getId() + 1;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.getIssuesGeneral(fakeProjectID));

        //then
        assertEquals(PROJECT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void getProductBacklog_happyPathTest() {

        //given
        Sprint sprint = SprintBuilder.createSprintEntity(defaultScrumProject, Boolean.FALSE);
        Issue issueInSprint = createIssueEntityWithSprint(sprint);
        sprintRepository.save(sprint);
        issueRepository.save(issueInSprint);
        IssueStatus status = issueStatusRepository.findById(1).orElse(null);
        Issue issueBeyondSprint = createIssueEntityWithIssueStatus(status);
        issueRepository.save(issueBeyondSprint);

        Backlog backlog = defaultScrumProject.getBacklog();
        Set<Issue> backlogIssues = backlog.getIssues();
        backlogIssues.add(issueBeyondSprint);
        backlogIssues.add(issueInSprint);
        backlogRepository.save(backlog);

        long expectedElementsAmountInProductBacklog = 1;

        //when
        List<IssueGeneralDTO> productBacklog = issueService.getProductBacklog(defaultScrumProject.getId());
        int actualElementsAmountInProductBacklog = productBacklog.size();
        IssueGeneralDTO backlogElement = productBacklog.stream().findAny().isPresent() ? productBacklog.stream().findAny().get() : null;

        //then
        assertNotNull(backlogElement);
        assertEquals(expectedElementsAmountInProductBacklog, actualElementsAmountInProductBacklog);
        assertEquals(issueBeyondSprint.getId(), backlogElement.getId());
        assertEquals(issueBeyondSprint.getName(), backlogElement.getName());
        assertEquals(issueBeyondSprint.getIssueStatus().getId(), backlogElement.getIssueStatusID());
        assertNull(issueBeyondSprint.getAssigneeID());
    }

    @Test
    public void getProductBacklog_throwsProjectDoesNotExistsExceptionTest() {

        //given
        Integer fakeProjectID = defaultScrumProject.getId() + 1;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.getProductBacklog(fakeProjectID));

        //then
        assertEquals(PROJECT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void getIssueDetails_happyPathTest() {

        //given
        when(userService.getUserByUUID(any())).thenReturn(createCurrentLoggedInUser());
        Issue issue = prepareIssueWithComments();
        int expectedCommentsArraySize = 1;

        //when
        IssueDTO actualReturnedIssueDetails = issueService.getIssueDetails(issue.getId());

        //then
        assertEquals(issue.getName(), actualReturnedIssueDetails.getName());
        assertEquals(issue.getDescription(), actualReturnedIssueDetails.getDescription());
        assertEquals(issue.getEstimation(), actualReturnedIssueDetails.getEstimation());
        assertEquals(expectedCommentsArraySize, actualReturnedIssueDetails.getComments().size());

        List<CommentDTO> issueComments = actualReturnedIssueDetails.getComments();
        CommentDTO comment = issueComments.get(0);

        assertEquals(DEFAULT_COMMENT_BODY, comment.getBody());
        assertEquals(createCurrentLoggedInUser(), comment.getAuthor());
    }

    @Test
    public void getIssueDetails_throwsIssueDoesNotExistsExceptionTest() {

        //given
        Integer fakeIssueID = -10;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.getIssueDetails(fakeIssueID));

        //then
        assertEquals(ISSUE_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void createIssue_happyPathTest() {

        //given
        Integer projectID = defaultScrumProject.getId();
        IssueMRO issueMRO = createDefaultIssue(projectID);
        long expectedIssueAmount = issueRepository.count() + 1;

        //when
        IssueDTO createdIssue = issueService.createIssue(issueMRO);
        long actualIssueAmount = issueRepository.count();

        //then
        assertEquals(expectedIssueAmount, actualIssueAmount);
        assertEquals(issueMRO.getName(), createdIssue.getName());
        assertEquals(issueMRO.getDescription(), createdIssue.getDescription());
        assertEquals(issueMRO.getEstimation(), createdIssue.getEstimation());
        assertEquals(issueMRO.getIssueTypeID(), createdIssue.getIssueTypeID());
        assertEquals(issueMRO.getIssuePriorityID(), createdIssue.getIssuePriorityID());
    }

    @Test
    public void createIssue_throwsProjectDoesNotExistsExceptionTest() {

        //given
        Integer fakeProjectID = defaultScrumProject.getId() + 1;
        IssueMRO issueMRO = createDefaultIssue(fakeProjectID);

        //when
        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> issueService.createIssue(issueMRO));

        //then
        assertEquals(PROJECT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void createIssue_throwsIssueTypeDoesNotExistsExceptionTest() {

        //given
        IssueMRO issueMRO = createIssueWithFakeIssueTypeID(defaultScrumProject.getId());

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.createIssue(issueMRO));

        //then
        assertEquals(ISSUE_TYPE_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void createIssue_throwsIssuePriorityDoesNotExistsExceptionTest() {

        //given
        IssueMRO issueMRO = createIssueWithFakeIssuePriorityID(defaultScrumProject.getId());

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.createIssue(issueMRO));

        //then
        assertEquals(ISSUE_PRIORITY_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void updateIssue_happyPathTest() {

        //given
        Issue issueToUpdate = createIssueEntity();
        issueRepository.save(issueToUpdate);

        IssueMRO updatedIssue = IssueMRO.builder()
                .issueID(issueToUpdate.getId())
                .name(OTHER_ISSUE_NAME)
                .description(OTHER_ISSUE_DESCRIPTION)
                .estimation(OTHER_ISSUE_ESTIMATION)
                .issuePriorityID(IssuePriorityID.TRIVIAL.getPriorityID())
                .build();

        //when
        IssueDTO issueAfterUpdate = issueService.updateIssue(updatedIssue);

        //then
        assertEquals(OTHER_ISSUE_NAME, issueAfterUpdate.getName());
        assertEquals(OTHER_ISSUE_DESCRIPTION, issueAfterUpdate.getDescription());
        assertEquals(OTHER_ISSUE_ESTIMATION, issueAfterUpdate.getEstimation());
        assertEquals(IssuePriorityID.TRIVIAL.getPriorityID(), issueAfterUpdate.getIssuePriorityID());
    }

    @Test
    public void updateIssue_throwsNullIdentifierExceptionTest() {

        //given
        IssueMRO issueWithNullIdentifier = new IssueMRO();

        //when
        Exception exception = assertThrows(IllegalArgumentException.class, () -> issueService.updateIssue(issueWithNullIdentifier));

        //then
        assertEquals(NULL_IDENTIFIER, exception.getMessage());
    }

    @Test
    public void updateIssue_throwsIssueDoesNotExistsExceptionTest() {

        //given
        Issue issueToUpdate = createIssueEntity();
        issueRepository.save(issueToUpdate);

        Integer fakeIssueID = issueToUpdate.getId() + 1;

        IssueMRO updatedIssueWithFakeIssueID = IssueMRO.builder()
                .issueID(fakeIssueID)
                .build();

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.updateIssue(updatedIssueWithFakeIssueID));

        //then
        assertEquals(ISSUE_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void updateIssue_throwsIssuePriorityDoesNotExistsExceptionTest() {

        //given
        Issue issueToUpdate = createIssueEntity();
        issueRepository.save(issueToUpdate);

        Integer fakeIssuePriorityID = 6;

        IssueMRO updatedIssueWithFakeIssuePriorityID = IssueMRO.builder()
                .issueID(issueToUpdate.getId())
                .issuePriorityID(fakeIssuePriorityID)
                .build();

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.updateIssue(updatedIssueWithFakeIssuePriorityID));

        //then
        assertEquals(ISSUE_PRIORITY_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void deleteIssue_happyPathTest() {

        //given
        Issue issue = createIssueEntity();
        issue.setProjectID(defaultScrumProject.getId());
        issueRepository.save(issue);
        long expectedIssuesAmount = issueRepository.count() - 1;

        //when
        issueService.deleteIssue(issue.getId());
        long actualIssuesAmount = issueRepository.count();

        //then
        assertEquals(expectedIssuesAmount, actualIssuesAmount);
    }

    @Test
    public void assignCurrentUserToIssue_happyPathTest() {

        //given
        Issue issue = createIssueEntity();
        issueRepository.save(issue);
        Integer issueID = issue.getId();
        String uuid = UUID.randomUUID().toString();
        when(userService.getCurrentLoggedUser()).thenReturn(UserBuilder.createAnotherCurrentLoggedInUser(uuid));

        //when
        issueService.assignCurrentUserToIssue(issueID);
        Issue actualIssueAfterAssignment = issueRepository.findById(issueID).isPresent() ? issueRepository.findById(issueID).get() : null;

        //then
        assertNotNull(actualIssueAfterAssignment);
        assertEquals(uuid, actualIssueAfterAssignment.getAssigneeID());
    }

    @Test
    public void assignCurrentUserToIssue_throwsIssueDoesNotExistsExceptionTest() {

        //given
        Integer fakeIssueID = -10;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.assignCurrentUserToIssue(fakeIssueID));

        //then
        assertEquals(ISSUE_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void deleteUserToIssueAssignment_happyPathTest() {

        //given
        Issue issue = createIssueEntity();
        issueRepository.save(issue);
        Integer issueID = issue.getId();
        String uuid = UUID.randomUUID().toString();
        when(userService.getCurrentLoggedUser()).thenReturn(UserBuilder.createAnotherCurrentLoggedInUser(uuid));
        issueService.assignCurrentUserToIssue(issueID);

        //when
        issueService.deleteUserToIssueAssignment(issueID);
        Issue actualIssueAfterAssignmentRemoval = issueRepository.findById(issueID).isPresent() ? issueRepository.findById(issueID).get() : null;

        //then
        assertNotNull(actualIssueAfterAssignmentRemoval);
        assertNull(actualIssueAfterAssignmentRemoval.getAssigneeID());
    }

    @Test
    public void deleteUserToIssueAssignment_throwsIssueDoesNotExistsExceptionTest() {

        //given
        Integer fakeIssueID = -10;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.deleteUserToIssueAssignment(fakeIssueID));

        //then
        assertEquals(ISSUE_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void getGeneralIssuesInfoBySprintID_happyPathTest() {

        //given
        long expectedIssuesInSprintAmount = 1;
        Sprint sprint = SprintBuilder.createSprintEntity(defaultScrumProject, Boolean.FALSE);
        Issue issue = IssueBuilder.createIssueEntityWithSprint(sprint);

        sprintRepository.save(sprint);
        issueRepository.save(issue);

        //when
        List<IssueGeneralDTO> sprintIssues = issueService.getGeneralIssuesInfoBySprintID(sprint.getId());
        int actualSprintIssuesAmount = sprintIssues.size();

        //then
        assertEquals(expectedIssuesInSprintAmount, actualSprintIssuesAmount);
    }

    @Test
    public void getGeneralIssuesInfoBySprintID_throwsSprintDoesNotExistsExceptionTest() {

        //given
        Integer fakeSprintID = -10;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.getGeneralIssuesInfoBySprintID(fakeSprintID));

        //then
        assertEquals(SPRINT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void changeIssueStatus_happyPathTest() {

        //given
        Sprint sprint = SprintBuilder.createSprintEntity(defaultScrumProject, Boolean.TRUE);
        Issue issue = createIssueEntityWithSprint(sprint);
        issue.setIssueStatus(issueStatusRepository.findById(IssueStatusID.TO_DO.getStatusID()).orElse(null));

        sprintRepository.save(sprint);
        issueRepository.save(issue);

        Integer inProgressStatusID = IssueStatusID.IN_PROGRESS.getStatusID();

        //when
        issueService.changeIssueStatus(issue.getId(), inProgressStatusID);
        Issue issueStateAfterStatusChange = issueRepository.findById(issue.getId()).orElse(null);

        //then
        assertNotNull(issueStateAfterStatusChange);
        assertEquals(inProgressStatusID, issueStateAfterStatusChange.getIssueStatus().getId());
    }

    @Test
    public void changeIssueStatus_throwsIssueDoesNotExistsExceptionTest() {

        //given
        Integer fakeIssueID = -10;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.changeIssueStatus(fakeIssueID, null));

        //then
        assertEquals(ISSUE_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void changeIssueStatus_throwsStatusChangeImpossibleSprintUnassignedExceptionTest() {

        //given
        Issue issue = createIssueEntity();
        issueRepository.save(issue);

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.changeIssueStatus(issue.getId(), null));

        //then
        assertEquals(IMPOSSIBLE_ISSUE_STATUS_CHANGE_SPRINT_EMPTY, exception.getMessage());
    }

    @Test
    public void changeIssueStatus_throwsStatusChangeImpossibleSprintNotActiveExceptionTest() {

        //given
        Sprint sprint = SprintBuilder.createSprintEntity(defaultScrumProject, Boolean.FALSE);
        Issue issue = createIssueEntityWithSprint(sprint);
        issue.setIssueStatus(issueStatusRepository.findById(IssueStatusID.TO_DO.getStatusID()).orElse(null));

        sprintRepository.save(sprint);
        issueRepository.save(issue);

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.changeIssueStatus(issue.getId(), null));

        //then
        assertEquals(IMPOSSIBLE_ISSUE_STATUS_CHANGE_SPRINT_NOT_ACTIVE, exception.getMessage());
    }

    @Test
    public void changeIssueStatus_throwsStatusChangeImpossibleGivenStatusDoesNotExistsExceptionTest() {

        //given
        Sprint sprint = SprintBuilder.createSprintEntity(defaultScrumProject, Boolean.TRUE);
        Issue issue = createIssueEntityWithSprint(sprint);
        issue.setIssueStatus(issueStatusRepository.findById(IssueStatusID.TO_DO.getStatusID()).orElse(null));

        sprintRepository.save(sprint);
        issueRepository.save(issue);

        Integer fakeIssueStatusID = 6;

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.changeIssueStatus(issue.getId(), fakeIssueStatusID));

        //then
        assertEquals(STATUS_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void changeIssueStatus_throwsStatusFlowOrderViolation_StatusChangeOfDONEIssue() {

        //given
        Sprint sprint = SprintBuilder.createSprintEntity(defaultScrumProject, Boolean.TRUE);
        Issue issue = createIssueEntityWithSprint(sprint);
        issue.setIssueStatus(issueStatusRepository.findById(IssueStatusID.DONE.getStatusID()).orElse(null));

        sprintRepository.save(sprint);
        issueRepository.save(issue);

        Integer toDOStatusID = IssueStatusID.TO_DO.getStatusID();

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.changeIssueStatus(issue.getId(), toDOStatusID));

        //then
        assertEquals(STATUS_FLOW_ORDER_VIOLATION, exception.getMessage());
    }

    @Test
    public void changeIssueStatus_throwsStatusFlowOrderViolation_UnnaturalStatusFlowOrder() {

        //given
        Sprint sprint = SprintBuilder.createSprintEntity(defaultScrumProject, Boolean.TRUE);
        Issue issue = createIssueEntityWithSprint(sprint);
        issue.setIssueStatus(issueStatusRepository.findById(IssueStatusID.IN_PROGRESS.getStatusID()).orElse(null));

        sprintRepository.save(sprint);
        issueRepository.save(issue);

        Integer doneStatusID = IssueStatusID.DONE.getStatusID();

        //when
        Exception exception = assertThrows(RuntimeException.class, () -> issueService.changeIssueStatus(issue.getId(), doneStatusID));

        //then
        assertEquals(STATUS_FLOW_ORDER_VIOLATION, exception.getMessage());
    }

    @Test
    public void changeIssueStatus_throwsStatusFlowOrderViolation_happyPath_IssueComesBackFromCodeReviewToInProgress() {

        //given
        Sprint sprint = SprintBuilder.createSprintEntity(defaultScrumProject, Boolean.TRUE);
        Issue issue = createIssueEntityWithSprint(sprint);
        issue.setIssueStatus(issueStatusRepository.findById(IssueStatusID.CODE_REVIEW.getStatusID()).orElse(null));

        sprintRepository.save(sprint);
        issueRepository.save(issue);

        Integer inProgressStatusID = IssueStatusID.IN_PROGRESS.getStatusID();

        //when
        issueService.changeIssueStatus(issue.getId(), inProgressStatusID);
        Issue issueStateAfterStatusChange = issueRepository.findById(issue.getId()).orElse(null);

        //then
        assertNotNull(issueStateAfterStatusChange);
        assertEquals(inProgressStatusID, issueStateAfterStatusChange.getIssueStatus().getId());
    }

    @Test
    public void changeIssueStatus_throwsStatusFlowOrderViolation_happyPath_IssueComesBackFromTestsToInProgress() {

        //given
        Sprint sprint = SprintBuilder.createSprintEntity(defaultScrumProject, Boolean.TRUE);
        Issue issue = createIssueEntityWithSprint(sprint);
        issue.setIssueStatus(issueStatusRepository.findById(IssueStatusID.IN_TESTS.getStatusID()).orElse(null));

        sprintRepository.save(sprint);
        issueRepository.save(issue);

        Integer inProgressStatusID = IssueStatusID.IN_PROGRESS.getStatusID();

        //when
        issueService.changeIssueStatus(issue.getId(), inProgressStatusID);
        Issue issueStateAfterStatusChange = issueRepository.findById(issue.getId()).orElse(null);

        //then
        assertNotNull(issueStateAfterStatusChange);
        assertEquals(inProgressStatusID, issueStateAfterStatusChange.getIssueStatus().getId());
    }

    @Test
    public void getGeneralIssuesInfoByUserID_returnsUserIssuesTest() {

        //given
        when(userService.getUserByUUID(any())).thenReturn(createCurrentLoggedInUser());
        Issue userIssue = issueRepository.save(createIssueEntityWithAssignee(CURRENT_USER_UUID));
        issueRepository.save(createIssueEntity());
        int expectedReturnedListSize = 1;

        //when
        List<IssueGeneralDTO> returedList = issueService.getGeneralIssuesInfoByUserID();
        int actualReturnedListSize = returedList.size();
        IssueGeneralDTO actualElement = returedList.get(0);

        //then
        assertEquals(expectedReturnedListSize, actualReturnedListSize);
        assertEquals(userIssue.getId(), actualElement.getId());
        assertEquals(userIssue.getName(), actualElement.getName());
        assertEquals(createCurrentLoggedInUser(), actualElement.getAssignee());
    }

    @Test
    public void getGeneralIssuesInfoByUserID_returnsEmptyListTest() {

        //given
        List<IssueGeneralDTO> emptyList = Collections.emptyList();
        when(userService.getUserByUUID(any())).thenReturn(createCurrentLoggedInUser());

        //when
        List<IssueGeneralDTO> returnedList = issueService.getGeneralIssuesInfoByUserID();

        //then
        assertArrayEquals(emptyList.toArray(), returnedList.toArray());
    }

    private Issue prepareIssueWithComments() {

        Date NOW = new Date();
        Comment comment = Comment.builder()
                .body(DEFAULT_COMMENT_BODY)
                .build();
        comment.setCreatedBy(CURRENT_USER_UUID);
        comment.setCreatedDate(NOW);
        comment.setLastModifiedBy(CURRENT_USER_UUID);
        comment.setLastModifiedDate(NOW);
        commentRepository.save(comment);

        Issue issueWithComment = createIssueEntity();
        Set<Comment> issueComments = issueWithComment.getComments();
        issueComments.add(comment);
        issueWithComment.setComments(issueComments);
        issueRepository.save(issueWithComment);

        return issueWithComment;
    }
}
