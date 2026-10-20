package io.github.student.agile.resource.service;

import io.github.student.agile.resource.domain.Backlog;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.dto.ProjectDTO;
import io.github.student.agile.resource.dto.ProjectDetailsDTO;
import io.github.student.agile.resource.dto.UserDTO;
import io.github.student.agile.resource.mapper.ProjectDTOMapper;
import io.github.student.agile.resource.repository.BacklogRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.ACTION_NOT_ALLOWED;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_NAME_NOT_UNIQUE;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.USERS_NUMBER_EXCEEDED;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectDTOMapper projectDTOMapper;
    private final BacklogRepository backlogRepository;
    private final UserService userService;

    @Override
    public ProjectDetailsDTO getProjectDetails(Integer projectID) {
        Project project = projectRepository.findById(projectID)
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));
        ProjectDetailsDTO detailsDTO = new ProjectDetailsDTO();
        detailsDTO.setProjectID(project.getId());
        detailsDTO.setName(project.getName());
        detailsDTO.setDescription(project.getDescription());
        detailsDTO.setProjectGoal(project.getProjectGoal());
        detailsDTO.setOwner(userService.getUserByUUID(project.getProjectOwner()));
        detailsDTO.setMembers(mapUserUUIDsToUserDTOs(project.getUserIDs()));
        return detailsDTO;
    }

    @Override
    public ProjectDTO createProject(ProjectDTO projectCreationRequest) {

        String currentLoggedUser = userService.getCurrentLoggedUser().getUuid();
        if (projectRepository.existsByNameAndProjectOwner(projectCreationRequest.getName(), currentLoggedUser)) {
            throw new RuntimeException(PROJECT_NAME_NOT_UNIQUE);
        }

        HashSet<String> usersAssociatedWithProject = new HashSet<>();
        usersAssociatedWithProject.add(currentLoggedUser);

        Backlog backlog = new Backlog();
        Project project = Project.builder()
                .name(projectCreationRequest.getName())
                .description(projectCreationRequest.getDescription())
                .projectOwner(currentLoggedUser)
                .projectGoal(projectCreationRequest.getProjectGoal())
                .backlog(backlog)
                .userIDs(usersAssociatedWithProject)
                .build();
        projectRepository.save(project);
        backlog.setProject(project);
        backlogRepository.save(backlog);

        return projectDTOMapper.map(project);
    }

    @Override
    public ProjectDTO updateProject(ProjectDTO projectDTO) {

        Project updated = projectRepository.findById(projectDTO.getProjectID())
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));
        updated.setName(projectDTO.getName());
        updated.setDescription(projectDTO.getDescription());
        updated.setProjectGoal(projectDTO.getProjectGoal());
        projectRepository.save(updated);
        return projectDTOMapper.map(updated);
    }

    @Override
    public ProjectDTO deleteProject(Integer projectID) {

        Project project = projectRepository.findById(projectID)
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));
        projectRepository.deleteById(projectID);
        return projectDTOMapper.map(project);
    }

    @Override
    public List<ProjectDTO> getAllProjects() {
        String currentLoggedUser = userService.getCurrentLoggedUser().getUuid();
        return projectRepository.findAllByUserIDsContains(currentLoggedUser)
                .stream()
                .map(projectDTOMapper::map)
                .collect(Collectors.toList());
    }

    @Override
    public void associateUserWithProject(String email, Integer projectID) {

        if (!userService.isProjectOwner(projectID)) {
            throw new RuntimeException(ACTION_NOT_ALLOWED);
        }

        Project updated = projectRepository.findById(projectID)
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));
        String userUUID = userService.getUserByEmail(email).getUuid();
        Set<String> usersAssociatedWithProject = updated.getUserIDs();

        Integer PROJECT_MAX_COLLABORATORS = 10;
        if (!PROJECT_MAX_COLLABORATORS.equals(usersAssociatedWithProject.size())) {
            usersAssociatedWithProject.add(userUUID);
            updated.setUserIDs(usersAssociatedWithProject);
            projectRepository.save(updated);
        } else {
            throw new RuntimeException(USERS_NUMBER_EXCEEDED);
        }
    }

    @Override
    public void deleteUserFromProject(String uuid, Integer projectID) {

        if (!userService.isProjectOwner(projectID)) {
            throw new RuntimeException(ACTION_NOT_ALLOWED);
        }

        Project updated = projectRepository.findById(projectID)
                .orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));

        // contains user existing verification
        String userUUID = userService.getUserByUUID(uuid).getUuid();
        Set<String> usersAssociatedWithProject = updated.getUserIDs();
        usersAssociatedWithProject.remove(userUUID);
        updated.setUserIDs(usersAssociatedWithProject);
        projectRepository.save(updated);
    }

    @Override
    public void deleteUserFromAllProjects(String uuid) {

        List<Project> allUsersProjects = projectRepository.findAllByUserIDsContains(uuid);
        if (!allUsersProjects.isEmpty()) {
            allUsersProjects.forEach(project -> {
                Set<String> projectUsers = project.getUserIDs();
                projectUsers.remove(uuid);
                project.setUserIDs(projectUsers);
                projectRepository.save(project);
            });
        }
    }

    private List<UserDTO> mapUserUUIDsToUserDTOs(Set<String> uuids) {
        return uuids.stream()
                .map(userService::getUserSafely)
                .collect(Collectors.toList());
    }
}
