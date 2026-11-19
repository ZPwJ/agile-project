package io.github.student.agile.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.dto.SprintDTO;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static io.github.student.agile.builder.IssueBuilder.createIssueEntity;
import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static io.github.student.agile.builder.SprintBuilder.SPRINT_GOAL;
import static io.github.student.agile.builder.SprintBuilder.SPRINT_NAME;
import static io.github.student.agile.builder.SprintBuilder.createSprint;
import static io.github.student.agile.builder.SprintBuilder.createSprintEntity;
import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.EMPTY_SPRINT;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.NO_ACTIVE_SPRINT_IN_PROJECT;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_ALREADY_FINISHED;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_NAME_NOT_UNIQUE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.TO_DO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@EnableJpaAuditing
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {TestConfiguration.class})
public class SprintRestControllerIntegrationTest {

    private static final String SPRINT_BASE_URL = "/api/sprint";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueStatusRepository issueStatusRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Project project;

    @BeforeEach
    void setUp() {
        when(userService.getCurrentLoggedUser()).thenReturn(createCurrentLoggedInUser());
        project = projectRepository.save(createProjectEntity());
    }

    @Test
    public void createSprint_returnsCreatedSprint() throws Exception {

        SprintDTO body = createSprint(project.getId());
        body.setSprintGoal(SPRINT_GOAL);

        mvc.perform(post(SPRINT_BASE_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(SPRINT_NAME)))
                .andExpect(jsonPath("$.sprintGoal", is(SPRINT_GOAL)))
                .andExpect(jsonPath("$.active", is(false)))
                .andExpect(jsonPath("$.projectID", is(project.getId())));

        assertThat(sprintRepository.findAll()).extracting(Sprint::getName).containsExactly(SPRINT_NAME);
    }

    @Test
    public void createSprint_nameTakenInProject_returns409() throws Exception {

        sprintRepository.save(createSprintEntity(project, Boolean.FALSE));

        mvc.perform(post(SPRINT_BASE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSprint(project.getId()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is(SPRINT_NAME_NOT_UNIQUE)));
    }

    @Test
    public void createSprint_notExistingProject_returns404() throws Exception {

        mvc.perform(post(SPRINT_BASE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSprint(-1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(PROJECT_DOES_NOT_EXISTS)));
    }

    @Test
    public void getActiveSprint_returnsActiveSprintOfProject() throws Exception {

        Sprint active = sprintRepository.save(createSprintEntity(project, Boolean.TRUE));

        mvc.perform(get(SPRINT_BASE_URL + "/active/{projectID}", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(active.getId())))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    public void getActiveSprint_noActiveSprint_returnsEmptyBody() throws Exception {

        mvc.perform(get(SPRINT_BASE_URL + "/active/{projectID}", project.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    public void getActiveSprint_notExistingProject_returns404() throws Exception {

        mvc.perform(get(SPRINT_BASE_URL + "/active/{projectID}", -1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(PROJECT_DOES_NOT_EXISTS)));
    }

    @Test
    public void getNonActivatedSprints_skipsActiveAndFinishedSprints() throws Exception {

        Sprint planned = sprintRepository.save(namedSprint("Planned", false, false));
        sprintRepository.save(namedSprint("Active", true, false));
        sprintRepository.save(namedSprint("Finished", false, true));

        mvc.perform(get(SPRINT_BASE_URL + "/non-activated/{projectID}", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(planned.getId())));
    }

    @Test
    public void updateSprint_returnsUpdatedSprint() throws Exception {

        Sprint sprint = sprintRepository.save(createSprintEntity(project, Boolean.FALSE));
        SprintDTO body = createSprint(project.getId());
        body.setId(sprint.getId());
        body.setName("Renamed sprint");
        body.setSprintGoal("New goal");

        mvc.perform(put(SPRINT_BASE_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sprint.getId())))
                .andExpect(jsonPath("$.name", is("Renamed sprint")))
                .andExpect(jsonPath("$.sprintGoal", is("New goal")));
    }

    @Test
    public void updateSprint_notExistingSprint_returns404() throws Exception {

        SprintDTO body = createSprint(project.getId());
        body.setId(-1);

        mvc.perform(put(SPRINT_BASE_URL).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(SPRINT_DOES_NOT_EXISTS)));
    }

    @Test
    public void addAndRemoveIssue_changesIssueSprint() throws Exception {

        Sprint sprint = sprintRepository.save(createSprintEntity(project, Boolean.FALSE));
        Issue issue = savedIssue();

        mvc.perform(post(SPRINT_BASE_URL + "/{sprintID}/issue/{issueID}", sprint.getId(), issue.getId()))
                .andExpect(status().isOk());
        assertThat(issueRepository.findById(issue.getId()).orElseThrow().getSprint()).isEqualTo(sprint);

        mvc.perform(delete(SPRINT_BASE_URL + "/{sprintID}/issue/delete/{issueID}", sprint.getId(), issue.getId()))
                .andExpect(status().isOk());
        assertThat(issueRepository.findById(issue.getId()).orElseThrow().getSprint()).isNull();
    }

    @Test
    public void addIssue_notExistingIssue_returns404() throws Exception {

        Sprint sprint = sprintRepository.save(createSprintEntity(project, Boolean.FALSE));

        mvc.perform(post(SPRINT_BASE_URL + "/{sprintID}/issue/{issueID}", sprint.getId(), -1))
                .andExpect(status().isNotFound());
    }

    @Test
    public void startAndStopSprint_changesSprintState() throws Exception {

        Sprint sprint = sprintRepository.save(createSprintEntity(project, Boolean.FALSE));
        mvc.perform(post(SPRINT_BASE_URL + "/{sprintID}/issue/{issueID}", sprint.getId(), savedIssue().getId()))
                .andExpect(status().isOk());

        mvc.perform(patch(SPRINT_BASE_URL + "/start/{id}", sprint.getId()))
                .andExpect(status().isOk());
        assertThat(sprintRepository.findById(sprint.getId()).orElseThrow().getActive()).isTrue();

        mvc.perform(patch(SPRINT_BASE_URL + "/project/{id}/stop", project.getId()))
                .andExpect(status().isOk());
        Sprint stopped = sprintRepository.findById(sprint.getId()).orElseThrow();
        assertThat(stopped.getActive()).isFalse();
        assertThat(stopped.getIsDone()).isTrue();
    }

    @Test
    public void startSprint_withoutIssues_returns409() throws Exception {

        Sprint sprint = sprintRepository.save(createSprintEntity(project, Boolean.FALSE));

        mvc.perform(patch(SPRINT_BASE_URL + "/start/{id}", sprint.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is(EMPTY_SPRINT)));
    }

    @Test
    public void stopSprint_noActiveSprint_returns409() throws Exception {

        mvc.perform(patch(SPRINT_BASE_URL + "/project/{id}/stop", project.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is(NO_ACTIVE_SPRINT_IN_PROJECT)));
    }

    @Test
    public void deleteSprint_removesSprint() throws Exception {

        Sprint sprint = sprintRepository.save(createSprintEntity(project, Boolean.FALSE));

        mvc.perform(delete(SPRINT_BASE_URL + "/{id}", sprint.getId()))
                .andExpect(status().isOk());

        assertThat(sprintRepository.findById(sprint.getId())).isEmpty();
    }

    @Test
    public void deleteSprint_finishedSprint_returns409() throws Exception {

        Sprint sprint = sprintRepository.save(namedSprint("Finished", false, true));

        mvc.perform(delete(SPRINT_BASE_URL + "/{id}", sprint.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is(SPRINT_ALREADY_FINISHED)));
    }

    private Sprint namedSprint(String name, boolean active, boolean done) {
        Sprint sprint = createSprintEntity(project, active);
        sprint.setName(name);
        sprint.setIsDone(done);
        return sprint;
    }

    private Issue savedIssue() {
        Issue issue = createIssueEntity();
        issue.setProjectID(project.getId());
        issue.setIssueStatus(issueStatusRepository.findById(TO_DO.getStatusID()).orElseThrow());
        return issueRepository.save(issue);
    }
}
