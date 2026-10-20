package io.github.student.agile.config;

import io.github.student.agile.resource.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@RequiredArgsConstructor
public class AuditingConfiguration {

    private final UserService userService;

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditAwareImpl(userService);
    }
}
