package io.github.student.agile.resource.service;

import io.github.student.agile.resource.dto.ProjectDTO;
import io.github.student.agile.resource.dto.ProjectDetailsDTO;

import java.util.List;

public interface ProjectService {

    ProjectDetailsDTO getProjectDetails(Integer projectID);
    ProjectDTO createProject(ProjectDTO projectDTO);
    ProjectDTO updateProject(ProjectDTO projectDTO);
    ProjectDTO deleteProject(Integer projectID);
    List<ProjectDTO> getAllProjects();
    void associateUserWithProject(String email, Integer projectID);
    void deleteUserFromProject(String uuid, Integer projectID);
    void deleteUserFromAllProjects(String uuid);
}
