package io.github.student.agile.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableJpaRepositories({"io.github.student.agile.resource.repository"})
@ComponentScan(value = {"io.github.student.agile.resource"})
@AutoConfigureDataJpa
@EntityScan({"io.github.student.agile.resource.domain"})
@EnableJpaAuditing
public class TestConfiguration {
}
