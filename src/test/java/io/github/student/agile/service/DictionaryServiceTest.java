package io.github.student.agile.service;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.IssuePriority;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.IssueType;
import io.github.student.agile.resource.repository.IssuePriorityRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.IssueTypeRepository;
import io.github.student.agile.resource.service.IssuePriorityService;
import io.github.student.agile.resource.service.IssueStatusService;
import io.github.student.agile.resource.service.IssueTypeService;
import io.github.student.agile.resource.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

@SpringBootTest(classes = TestConfiguration.class)
@Transactional
public class DictionaryServiceTest {

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    @Autowired
    private IssueStatusRepository issueStatusRepository;

    @Autowired
    private IssuePriorityRepository issuePriorityRepository;

    @Autowired
    private IssueTypeService issueTypeService;

    @Autowired
    private IssueStatusService issueStatusService;

    @Autowired
    private IssuePriorityService issuePriorityService;

    @MockBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        Mockito.when(userService.getCurrentLoggedUser()).thenReturn(createCurrentLoggedInUser());
    }

    @Test
    public void getAllIssueTypesTest() {

        //given
        long expectedTypesAmount = issueTypeRepository.count();

        //when
        List<IssueType> allTypes = issueTypeService.getAllIssueTypes();
        int actualTypesAmount = allTypes.size();

        //then
        assertEquals(expectedTypesAmount, actualTypesAmount);
        assertIterableEquals(issueTypeRepository.findAll(), allTypes);
    }

    @Test
    public void getAllIssueStatusesTest() {

        //given
        long expectedStatusesAmount = issueStatusRepository.count();

        //when
        List<IssueStatus> allStatuses = issueStatusService.getAllIssueStatuses();
        int actualStatusesAmount = allStatuses.size();

        //then
        assertEquals(expectedStatusesAmount, actualStatusesAmount);
        assertIterableEquals(issueStatusRepository.findAll(), allStatuses);
    }

    @Test
    public void getAllIssuePrioritiesTest() {

        //given
        long expectedPrioritiesAmount = issuePriorityRepository.count();

        //when
        List<IssuePriority> allPriorities = issuePriorityService.getAllIssuePriorities();
        int actualPrioritiesAmount = allPriorities.size();

        //then
        assertEquals(expectedPrioritiesAmount, actualPrioritiesAmount);
        assertIterableEquals(issuePriorityRepository.findAll(), allPriorities);
    }
}
