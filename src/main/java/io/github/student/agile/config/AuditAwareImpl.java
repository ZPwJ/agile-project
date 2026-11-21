package io.github.student.agile.config;

import io.github.student.agile.resource.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuditAwareImpl implements AuditorAware<String> {

    private final UserService userService;

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of(userService.getCurrentLoggedUser().getUuid());
    }
}
