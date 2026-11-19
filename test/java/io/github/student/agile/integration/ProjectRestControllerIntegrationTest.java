package io.github.student.agile.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.Backlog;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.dto.ProjectDTO;
import io.github.student.agile.resource.dto.UserAssociationDTO;
import io.github.student.agile.resource.dto.UserDTO;
import io.github.student.agile.resource.mapper.ProjectDTOMapper;
import io.github.student.agile.resource.repository.ProjectRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.github.student.agile.builder.ProjectBuilder.ANOTHER_PROJECT_DESCRIPTION;
import static io.github.student.agile.builder.ProjectBuilder.ANOTHER_PROJECT_GOAL;
import static io.github.student.agile.builder.ProjectBuilder.ANOTHER_PROJECT_NAME;
import static io.github.student.agile.builder.ProjectBuilder.PROJECT_DESCRIPTION;
import static io.github.student.agile.builder.ProjectBuilder.PROJECT_GOAL;
import static io.github.student.agile.builder.ProjectBuilder.PROJECT_NAME;
import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;
import static io.github.student.agile.builder.UserBuilder.prepareAnotherUserMock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@EnableJpaAuditing
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@EnableAutoConfiguration(exclude= SecurityAutoConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {TestConfiguration.class})
public class ProjectRestControllerIntegrationTest {

    private static final String PROJECT_CONTROLLER_BASE_URL = "/api/scrum-project";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectDTOMapper projectDTOMapper;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper();
        when(userService.getCurrentLoggedUser()).thenReturn(createCurrentLoggedInUser());
    }

    @Test
    @WithMockUser(username = "scrum_master", roles = "sm")
    public void createProject_returnsProjectDTOResponseAndHttpStatusCodeCreated() throws Exception {

        mvc.perform(post(PROJECT_CONTROLLER_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDefaultProjectCreationBody())))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(PROJECT_NAME)))
                .andExpect(jsonPath("$.description", is(PROJECT_DESCRIPTION)))
                .andExpect(jsonPath("$.projectGoal", is(PROJECT_GOAL)));

        List<Project> allProjects = projectRepository.findAll();
        assertThat(allProjects).extracting(Project::getName).containsOnly(PROJECT_NAME);
    }

    @Test
    @WithMockUser(username = "plain_user", roles = "dev")
    public void getAllProjects_returnsProjectDTOResponseAndHttpStatusCode200() throws Exception {

        String currentLoggedInUserUUID = createCurrentLoggedInUser().getUuid();
        Project project = createDefaultProject(Set.of(currentLoggedInUserUUID), currentLoggedInUserUUID);

        mvc.perform(get(PROJECT_CONTROLLER_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].projectID", is(project.getId())))
                .andExpect(jsonPath("$[0].name", is(project.getName())))
                .andExpect(jsonPath("$[0].description", is(project.getDescription())))
                .andExpect(jsonPath("$[0].projectGoal", is(project.getProjectGoal())));
    }

    @Test
    @WithMockUser(value = "scrum_master", roles = "sm")
    public void updateProject_returnsProjectDTOResponseAndHttpStatusCode200() throws Exception {

        String currentLoggedInUserUUID = createCurrentLoggedInUser().getUuid();
        Project project = createDefaultProject(Set.of(currentLoggedInUserUUID), currentLoggedInUserUUID);

        ProjectDTO requestBody = projectDTOMapper.map(project);
        requestBody.setName(ANOTHER_PROJECT_NAME);
        requestBody.setDescription(ANOTHER_PROJECT_DESCRIPTION);
        requestBody.setProjectGoal(ANOTHER_PROJECT_GOAL);

        mvc.perform(put(PROJECT_CONTROLLER_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectID", is(project.getId())))
                .andExpect(jsonPath("$.name", is(ANOTHER_PROJECT_NAME)))
                .andExpect(jsonPath("$.description", is(ANOTHER_PROJECT_DESCRIPTION)))
                .andExpect(jsonPath("$.projectGoal", is(ANOTHER_PROJECT_GOAL)));

        List<Project> allProjects = projectRepository.findAll();
        assertThat(allProjects).extracting(Project::getName).containsOnly(ANOTHER_PROJECT_NAME);
    }

    @Test
    @WithMockUser(username = "scrum_master", roles = "sm")
    public void deleteProject_returnsProjectDTOResponseAndHttpStatusCode200() throws Exception {

        String currentLoggedInUserUUID = createCurrentLoggedInUser().getUuid();
        Project project = createDefaultProject(Set.of(currentLoggedInUserUUID), currentLoggedInUserUUID);

        mvc.perform(delete(PROJECT_CONTROLLER_BASE_URL.concat("/").concat(String.valueOf(project.getId().intValue())))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectID", is(project.getId())))
                .andExpect(jsonPath("$.name", is(PROJECT_NAME)))
                .andExpect(jsonPath("$.description", is(PROJECT_DESCRIPTION)))
                .andExpect(jsonPath("$.projectGoal", is(PROJECT_GOAL)));

        List<Project> allProjects = projectRepository.findAll();
        assertThat(allProjects).isEmpty();
    }

    @Test
    @WithMockUser(username = "scrum_master", roles = "sm")
    public void associateUserWithProject_returnsHttpStatusCode200() throws Exception {

        UserDTO anotherUser = prepareAnotherUserMock();
        when(userService.isProjectOwner(any())).thenReturn(Boolean.TRUE);
        when(userService.getUserByEmail(any())).thenReturn(anotherUser);

        String currentLoggedInUserUUID = createCurrentLoggedInUser().getUuid();
        Project project = createDefaultProject(Set.of(currentLoggedInUserUUID), currentLoggedInUserUUID);

        UserAssociationDTO associationRequestBody = new UserAssociationDTO();
        associationRequestBody.setEmail(anotherUser.getEmail());
        associationRequestBody.setProjectID(project.getId());

        mvc.perform(post(PROJECT_CONTROLLER_BASE_URL.concat("/user-association"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(associationRequestBody)))
                .andDo(print())
                .andExpect(status().isOk());

        assertThat(project.getUserIDs()).hasSize(2).containsOnly(currentLoggedInUserUUID, anotherUser.getUuid());
    }

    private ProjectDTO createDefaultProjectCreationBody() {

        return ProjectDTO.builder()
                .name(PROJECT_NAME)
                .description(PROJECT_DESCRIPTION)
                .projectGoal(PROJECT_GOAL)
                .build();
    }

    private Project createDefaultProject(Set<String> users, String ownerID) {

        Project project = Project.builder()
                .name(PROJECT_NAME)
                .description(PROJECT_DESCRIPTION)
                .projectGoal(PROJECT_GOAL)
                .sprints(new HashSet<>())
                .backlog(new Backlog())
                .userIDs(new HashSet<>(users))
                .projectOwner(ownerID)
                .build();
        return projectRepository.save(project);
    }
}
