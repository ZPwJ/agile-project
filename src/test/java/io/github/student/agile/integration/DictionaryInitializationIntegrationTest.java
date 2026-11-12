package io.github.student.agile.integration;

import io.github.student.agile.config.TestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestConfiguration.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:dictionary-init;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.sql.init.data-locations=classpath:data-postgres.sql"
})
@Transactional
class DictionaryInitializationIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void initializesAllDictionariesWithStableIdentifiers() {
        assertDictionaries(new JdbcTemplate(dataSource));
    }

    @Test
    void repeatedInitializationKeepsDictionariesAndExistingProject() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO project (projectid, name) VALUES (?, ?)", 99999, "Existing project");
        ResourceDatabasePopulator initializer = new ResourceDatabasePopulator(
                new ClassPathResource("data-postgres.sql"));

        initializer.execute(dataSource);
        initializer.execute(dataSource);

        assertDictionaries(jdbc);
        assertThat(jdbc.queryForObject("SELECT name FROM project WHERE projectid = 99999", String.class))
                .isEqualTo("Existing project");
    }

    private void assertDictionaries(JdbcTemplate jdbc) {
        assertThat(jdbc.queryForList("SELECT status_name FROM issue_status ORDER BY statusid", String.class))
                .containsExactly("TO_DO", "IN_PROGRESS", "CODE_REVIEW", "IN_TESTS", "DONE");
        assertThat(jdbc.queryForList("SELECT statusid FROM issue_status ORDER BY statusid", Integer.class))
                .containsExactly(1, 2, 3, 4, 5);
        assertThat(jdbc.queryForList("SELECT type FROM issue_type ORDER BY issue_typeid", String.class))
                .containsExactly("USER_STORY", "BUG", "TO_DO", "AOA");
        assertThat(jdbc.queryForList("SELECT issue_typeid FROM issue_type ORDER BY issue_typeid", Integer.class))
                .containsExactly(1, 2, 3, 4);
        assertThat(jdbc.queryForList("SELECT priority FROM issue_priority ORDER BY issue_priorityid", String.class))
                .containsExactly("CRITICAL", "HIGH", "MEDIUM", "LOW", "TRIVIAL");
        assertThat(jdbc.queryForList("SELECT issue_priorityid FROM issue_priority ORDER BY issue_priorityid", Integer.class))
                .containsExactly(1, 2, 3, 4, 5);
    }
}
