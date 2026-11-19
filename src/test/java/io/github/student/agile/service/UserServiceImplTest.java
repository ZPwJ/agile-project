package io.github.student.agile.service;

import io.github.student.agile.exception.definition.ProjectAccessDeniedException;
import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.service.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.ACTION_NOT_ALLOWED;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    private static final Integer PROJECT_ID = 10;
    private static final String OWNER_UUID = "owner-uuid";
    private static final String MEMBER_UUID = "member-uuid";
    private static final String OUTSIDER_UUID = "outsider-uuid";

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void hasProjectAccess_returnsTrueForProjectOwner() {
        authenticateAs(OWNER_UUID);
        Project project = Project.builder()
                .id(PROJECT_ID)
                .projectOwner(OWNER_UUID)
                .userIDs(Set.of(MEMBER_UUID))
                .build();
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        assertTrue(userService.hasProjectAccess(PROJECT_ID));
    }

    @Test
    public void hasProjectAccess_returnsTrueForProjectMember() {
        authenticateAs(MEMBER_UUID);
        Project project = Project.builder()
                .id(PROJECT_ID)
                .projectOwner(OWNER_UUID)
                .userIDs(Set.of(OWNER_UUID, MEMBER_UUID))
                .build();
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        assertTrue(userService.hasProjectAccess(PROJECT_ID));
    }

    @Test
    public void hasProjectAccess_returnsFalseForNonMember() {
        authenticateAs(OUTSIDER_UUID);
        Project project = Project.builder()
                .id(PROJECT_ID)
                .projectOwner(OWNER_UUID)
                .userIDs(Set.of(OWNER_UUID, MEMBER_UUID))
                .build();
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        assertFalse(userService.hasProjectAccess(PROJECT_ID));
    }

    @Test
    public void hasProjectAccess_returnsFalseWhenMembersSetIsNull() {
        authenticateAs(OUTSIDER_UUID);
        Project project = Project.builder()
                .id(PROJECT_ID)
                .projectOwner(OWNER_UUID)
                .userIDs(null)
                .build();
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        assertFalse(userService.hasProjectAccess(PROJECT_ID));
    }

    @Test
    public void hasProjectAccess_throwsResourceNotFoundWhenProjectMissing() {
        authenticateAs(MEMBER_UUID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.hasProjectAccess(PROJECT_ID)
        );
        assertEquals(PROJECT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void verifyProjectAccess_succeedsForMemberAndOwner() {
        Project project = Project.builder()
                .id(PROJECT_ID)
                .projectOwner(OWNER_UUID)
                .userIDs(Set.of(OWNER_UUID, MEMBER_UUID))
                .build();
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        authenticateAs(OWNER_UUID);
        assertDoesNotThrow(() -> userService.verifyProjectAccess(PROJECT_ID));

        authenticateAs(MEMBER_UUID);
        assertDoesNotThrow(() -> userService.verifyProjectAccess(PROJECT_ID));
    }

    @Test
    public void verifyProjectAccess_throwsProjectAccessDeniedForNonMember() {
        authenticateAs(OUTSIDER_UUID);
        Project project = Project.builder()
                .id(PROJECT_ID)
                .projectOwner(OWNER_UUID)
                .userIDs(Set.of(OWNER_UUID, MEMBER_UUID))
                .build();
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        ProjectAccessDeniedException exception = assertThrows(
                ProjectAccessDeniedException.class,
                () -> userService.verifyProjectAccess(PROJECT_ID)
        );
        assertEquals(ACTION_NOT_ALLOWED, exception.getMessage());
    }

    @Test
    public void verifyProjectAccess_throwsResourceNotFoundWhenProjectMissing() {
        authenticateAs(OUTSIDER_UUID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.verifyProjectAccess(PROJECT_ID)
        );
        assertEquals(PROJECT_DOES_NOT_EXISTS, exception.getMessage());
    }

    @Test
    public void isProjectOwner_checksOwnerEqualityAndThrowsNotFound() {
        Project project = Project.builder()
                .id(PROJECT_ID)
                .projectOwner(OWNER_UUID)
                .userIDs(Set.of(OWNER_UUID, MEMBER_UUID))
                .build();
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        authenticateAs(OWNER_UUID);
        assertTrue(userService.isProjectOwner(PROJECT_ID));

        authenticateAs(MEMBER_UUID);
        assertFalse(userService.isProjectOwner(PROJECT_ID));
    }

    @Test
    public void hasProjectAccess_throwsIllegalStateWhenAuthenticationMissing() {
        Project project = Project.builder()
                .id(PROJECT_ID)
                .projectOwner(OWNER_UUID)
                .userIDs(Set.of(OWNER_UUID))
                .build();
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        assertThrows(IllegalStateException.class, () -> userService.hasProjectAccess(PROJECT_ID));
    }

    @Test
    public void getAllProjectUsersUUIDs_throwsResourceNotFoundWhenProjectMissing() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getAllProjectUsersUUIDs(PROJECT_ID)
        );
        assertEquals(PROJECT_DOES_NOT_EXISTS, exception.getMessage());
    }

    private void authenticateAs(String username) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("preferred_username", username)
                .claim("email", username + "@example.com")
                .claim("given_name", "Test")
                .claim("family_name", "User")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
