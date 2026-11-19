package io.github.student.agile.integration;

import io.github.student.agile.config.TestConfiguration;
import io.github.student.agile.resource.service.UserService;
import jakarta.transaction.Transactional;
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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@EnableJpaAuditing
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {TestConfiguration.class})
public class IssueDictionaryRestControllerIntegrationTest {

    private static final String DICTIONARY_BASE_URL = "/api/dictionary";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @Test
    public void getAllIssueStatuses_returnsAllStatuses() throws Exception {

        mvc.perform(get(DICTIONARY_BASE_URL + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(1, 2, 3, 4, 5)));
    }

    @Test
    public void getAllIssueTypes_returnsTypes() throws Exception {

        mvc.perform(get(DICTIONARY_BASE_URL + "/type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItems(1, 2, 3)));
    }

    @Test
    public void getAllIssuePriorities_returnsPriorities() throws Exception {

        mvc.perform(get(DICTIONARY_BASE_URL + "/priority"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItems(1, 2, 3, 4, 5)));
    }
}
