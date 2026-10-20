package io.github.student.agile.resource.service;

import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.dto.UserDTO;
import io.github.student.agile.resource.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Set;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final ProjectRepository projectRepository;

    @Override
    public UserDTO getCurrentLoggedUser() {
        Jwt jwt = currentJwt();
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");

        return UserDTO.builder()
                .uuid(username)
                .email(isNull(email) ? username : email)
                .firstName(jwt.getClaimAsString("given_name"))
                .lastName(jwt.getClaimAsString("family_name"))
                .build();
    }

    @Override
    public UserDTO getUserByEmail(String email) {
        return UserDTO.builder()
                .uuid(email)
                .email(email)
                .firstName("Unknown")
                .lastName("Unknown")
                .build();
    }

    @Override
    public UserDTO getUserByUUID(String uuid) {
        return UserDTO.builder()
                .uuid(uuid)
                .email(uuid)
                .firstName("Unknown")
                .lastName("Unknown")
                .build();
    }

    @Override
    public UserDTO getUserSafely(String uuid) {
        if (isNull(uuid)) {
            return null;
        }
        return getUserByUUID(uuid);
    }

    @Override
    public boolean isProjectOwner(Integer projectID) {
        String currentLoggedUserUUID = currentJwt().getClaimAsString("preferred_username");
        Project project = projectRepository.findById(projectID).orElseThrow(RuntimeException::new);
        String projectOwnerUUID = project.getProjectOwner();
        return projectOwnerUUID.equals(currentLoggedUserUUID);
    }

    @Override
    public boolean isAnyProjectOwner(String uuid) {
        return projectRepository.existsByProjectOwner(uuid);
    }

    @Override
    public Set<String> getAllProjectUsersUUIDs(Integer projectID) {

        Project project = projectRepository.findById(projectID)
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));

        return project.getUserIDs();
    }

    private Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("Current authentication is not available");
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken();
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        throw new IllegalStateException("Current authentication does not contain a JWT");
    }
}
