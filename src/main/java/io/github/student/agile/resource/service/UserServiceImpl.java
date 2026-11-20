package io.github.student.agile.resource.service;

import io.github.student.agile.exception.definition.ProjectAccessDeniedException;
import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.dto.UserDTO;
import io.github.student.agile.resource.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.ACTION_NOT_ALLOWED;
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
        String currentLoggedUserUUID = currentUserUuid();
        Project project = findProjectOrThrow(projectID);
        return Objects.equals(project.getProjectOwner(), currentLoggedUserUUID);
    }

    @Override
    public boolean isAnyProjectOwner(String uuid) {
        return projectRepository.existsByProjectOwner(uuid);
    }

    @Override
    public boolean hasProjectAccess(Integer projectID) {
        Project project = findProjectOrThrow(projectID);
        String currentUserUUID = currentUserUuid();
        if (Objects.equals(project.getProjectOwner(), currentUserUUID)) {
            return true;
        }
        Set<String> members = project.getUserIDs();
        return members != null && members.contains(currentUserUUID);
    }

    @Override
    public void verifyProjectAccess(Integer projectID) {
        if (!hasProjectAccess(projectID)) {
            throw new ProjectAccessDeniedException(ACTION_NOT_ALLOWED);
        }
    }

    @Override
    public Set<String> getAllProjectUsersUUIDs(Integer projectID) {
        return findProjectOrThrow(projectID).getUserIDs();
    }

    private Project findProjectOrThrow(Integer projectID) {
        return projectRepository.findById(projectID)
                .orElseThrow(() -> new ResourceNotFoundException(PROJECT_DOES_NOT_EXISTS));
    }

    private String currentUserUuid() {
        UserDTO currentUser = getCurrentLoggedUser();
        if (currentUser == null || currentUser.getUuid() == null) {
            throw new IllegalStateException("Current user identifier is not available");
        }
        return currentUser.getUuid();
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
