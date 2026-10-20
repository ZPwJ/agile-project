package io.github.student.agile.resource.controller;

import io.github.student.agile.resource.dto.SprintDTO;
import io.github.student.agile.resource.service.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sprint")
@RequiredArgsConstructor
public class SprintRestController {

    private final SprintService sprintService;

    @GetMapping("/active/{projectID}")
    public ResponseEntity<SprintDTO> getActiveSprint(@PathVariable Integer projectID) {
        return ResponseEntity.ok(sprintService.getActiveSprint(projectID));
    }

    @GetMapping("/non-activated/{projectID}")
    public ResponseEntity<List<SprintDTO>> getAllSprints(@PathVariable Integer projectID) {
        return ResponseEntity.ok(sprintService.getAllNonActivatedSprints(projectID));
    }

    @PostMapping
    public ResponseEntity<SprintDTO> createSprint(@RequestBody SprintDTO sprintDTO) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(sprintService.createSprint(sprintDTO));
    }

    @PutMapping
    public ResponseEntity<SprintDTO> updateSprint(@RequestBody SprintDTO sprintDTO) {
        return ResponseEntity.ok(sprintService.updateSprint(sprintDTO));
    }

    @PostMapping("/{sprintID}/issue/{issueID}")
    public void addIssueToSprint(@PathVariable Integer sprintID, @PathVariable Integer issueID) {
        sprintService.addIssueToSprint(issueID, sprintID);
    }

    @DeleteMapping("/{sprintID}/issue/delete/{issueID}")
    public void deleteIssueFromSprint(@PathVariable Integer sprintID, @PathVariable Integer issueID) {
        sprintService.deleteIssueFromSprint(sprintID, issueID);
    }

    @DeleteMapping("/{id}")
    public void deleteSprint(@PathVariable Integer id) {
        sprintService.deleteSprint(id);
    }

    @PatchMapping("/start/{id}")
    public void startSprint(@PathVariable Integer id) {
        sprintService.startSprint(id);
    }

    @PatchMapping("/project/{id}/stop")
    public void stopSprint(@PathVariable Integer id) {
        sprintService.stopSprint(id);
    }
}
