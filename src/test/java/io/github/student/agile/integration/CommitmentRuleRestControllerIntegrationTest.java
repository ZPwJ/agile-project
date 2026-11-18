package io.github.student.agile.integration;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.domain.CommitmentRule;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.repository.CommitmentRuleRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.service.CommitmentRuleService;
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
import org.springframework.test.web.servlet.MockMvc;

import static io.github.student.agile.builder.ProjectBuilder.createProjectEntity;
import static io.github.student.agile.builder.UserBuilder.createCurrentLoggedInUser;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@EnableJpaAuditing
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {TestConfiguration.class})
public class CommitmentRuleRestControllerIntegrationTest {

    private static final String DOD_BASE_URL = "/api/commitment/dod";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CommitmentRuleRepository ruleRepository;

    @Autowired
    private CommitmentRuleService ruleService;

    private Project project;

    @BeforeEach
    void setUp() {
        when(userService.getCurrentLoggedUser()).thenReturn(createCurrentLoggedInUser());
        project = projectRepository.save(createProjectEntity());
    }

    @Test
    public void defineRule_returnsRuleAndAddsItToProject() throws Exception {

        mvc.perform(post(DOD_BASE_URL + "/project/{projectID}/rule", project.getId()).param("rule", "Code reviewed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleName", is("Code reviewed")));

        assertThat(projectRepository.findById(project.getId()).orElseThrow().getDod())
                .extracting(CommitmentRule::getRule).containsExactly("Code reviewed");
    }

    @Test
    public void getAllRules_returnsRulesOfProject() throws Exception {

        ruleService.defineRule(project.getId(), "Code reviewed");
        ruleService.defineRule(project.getId(), "Tests pass");

        mvc.perform(get(DOD_BASE_URL + "/project/{projectID}", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].ruleName", containsInAnyOrder("Code reviewed", "Tests pass")));
    }

    @Test
    public void updateRule_returnsUpdatedRule() throws Exception {

        Integer ruleID = ruleService.defineRule(project.getId(), "Code reviewed").getRuleID();

        mvc.perform(patch(DOD_BASE_URL + "/rule/{ruleID}", ruleID).param("rule", "Code reviewed by two people"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleID", is(ruleID)))
                .andExpect(jsonPath("$.ruleName", is("Code reviewed by two people")));
    }

    @Test
    public void deleteRule_removesSingleRule() throws Exception {

        Integer removed = ruleService.defineRule(project.getId(), "Code reviewed").getRuleID();
        ruleService.defineRule(project.getId(), "Tests pass");

        mvc.perform(delete(DOD_BASE_URL + "/project/{projectID}/rule/{ruleID}", project.getId(), removed))
                .andExpect(status().isOk());

        assertThat(ruleRepository.findById(removed)).isEmpty();
        assertThat(projectRepository.findById(project.getId()).orElseThrow().getDod())
                .extracting(CommitmentRule::getRule).containsExactly("Tests pass");
    }

    @Test
    public void deleteAllRules_removesEveryRuleOfProject() throws Exception {

        ruleService.defineRule(project.getId(), "Code reviewed");
        ruleService.defineRule(project.getId(), "Tests pass");

        mvc.perform(delete(DOD_BASE_URL + "/project/{projectID}/rule", project.getId()))
                .andExpect(status().isOk());

        assertThat(projectRepository.findById(project.getId()).orElseThrow().getDod()).isEmpty();
        assertThat(ruleRepository.findAll()).isEmpty();
    }

    @Test
    public void projectRules_notExistingProject_returns404() throws Exception {

        mvc.perform(get(DOD_BASE_URL + "/project/{projectID}", -1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(PROJECT_DOES_NOT_EXISTS)));
        mvc.perform(post(DOD_BASE_URL + "/project/{projectID}/rule", -1).param("rule", "Code reviewed"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(DOD_BASE_URL + "/project/{projectID}/rule", -1))
                .andExpect(status().isNotFound());
    }

    @Test
    public void defineRule_withoutRuleParameter_returns400() throws Exception {

        mvc.perform(post(DOD_BASE_URL + "/project/{projectID}/rule", project.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }
}
