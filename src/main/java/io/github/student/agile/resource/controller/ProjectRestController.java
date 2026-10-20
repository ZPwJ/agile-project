package io.github.student.agile.resource.controller;

import io.github.student.agile.resource.dto.ProjectDTO;
import io.github.student.agile.resource.dto.ProjectDetailsDTO;
import io.github.student.agile.resource.dto.UserAssociationDTO;
import io.github.student.agile.resource.dto.UserExcludingDTO;
import io.github.student.agile.resource.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scrum-project")
@RequiredArgsConstructor
public class ProjectRestController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @PostMapping
    @PreAuthorize("hasRole('sm')")
    public ResponseEntity<ProjectDTO> createProject(@RequestBody ProjectDTO project) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(projectService.createProject(project));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDetailsDTO> getProjectDetailsByProjectID(@PathVariable Integer id) {
        return ResponseEntity.ok(projectService.getProjectDetails(id));
    }

    @PutMapping
    @PreAuthorize("hasRole('sm')")
    public ResponseEntity<ProjectDTO> updateProject(@RequestBody ProjectDTO project) {
        return ResponseEntity.ok(projectService.updateProject(project));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('sm')")
    public ResponseEntity<ProjectDTO> deleteProject(@PathVariable Integer id) {
        return ResponseEntity.ok(projectService.deleteProject(id));
    }

    @PostMapping("/user-association")
    @PreAuthorize("hasRole('sm')")
    public void associateUserWithProject(@RequestBody UserAssociationDTO associationRequest) {
        projectService.associateUserWithProject(associationRequest.getEmail(), associationRequest.getProjectID());
    }

    @DeleteMapping("/delete-user")
    @PreAuthorize("hasRole('sm')")
    public void deleteUserFromProject(@RequestBody UserExcludingDTO exclusionRequest) {
        projectService.deleteUserFromProject(exclusionRequest.getUserUUID(), exclusionRequest.getProjectID());
    }
}
