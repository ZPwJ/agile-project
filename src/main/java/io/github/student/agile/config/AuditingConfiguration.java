
package io.github.student.agile.config;

import io.github.student.agile.resource.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@RequiredArgsConstructor
public class AuditingConfiguration {

    private final UserService userService;

    @Bean("auditorProvider")
    @Profile("!demo")
    public AuditorAware<String> auditorProvider() {
        return new AuditAwareImpl(userService);
    }

    @Bean("auditorProvider")
    @Profile("demo")
    public AuditorAware<String> demoAuditorProvider() {
        return () -> Optional.of("demo");
    }
}
