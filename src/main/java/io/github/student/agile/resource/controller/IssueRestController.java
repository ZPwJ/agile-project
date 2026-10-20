package io.github.student.agile.resource.controller;

import io.github.student.agile.resource.dto.IssueDTO;
import io.github.student.agile.resource.dto.IssueGeneralDTO;
import io.github.student.agile.resource.dto.request.IssueMRO;
import io.github.student.agile.resource.service.IssueService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/issue")
@RequiredArgsConstructor
public class IssueRestController {

    private final IssueService issueService;

    @GetMapping
    public ResponseEntity<List<IssueGeneralDTO>> getGeneralIssuesInfoByProjectID(@RequestParam Integer projectID) {
        return ResponseEntity.ok(issueService.getIssuesGeneral(projectID));
    }

    @GetMapping("/product-backlog")
    public ResponseEntity<List<IssueGeneralDTO>> getProductBacklogByProjectID(@RequestParam Integer projectID) {
        return ResponseEntity.ok(issueService.getProductBacklog(projectID));
    }

    @GetMapping("/product-backlog-history")
    public ResponseEntity<List<IssueGeneralDTO>> getProductBacklogHistoryByProjectID(@RequestParam Integer projectID) {
        return ResponseEntity.ok(issueService.getBacklogHistory(projectID));
    }

    @GetMapping("/user-assigned")
    public ResponseEntity<List<IssueGeneralDTO>> getGeneralIssuesInfoByUserID() {
        return ResponseEntity.ok(issueService.getGeneralIssuesInfoByUserID());
    }

    @GetMapping("/sprint/{id}")
    public ResponseEntity<List<IssueGeneralDTO>> getGeneralIssuesInfoBySprintID(@PathVariable Integer id) {
        return ResponseEntity.ok(issueService.getGeneralIssuesInfoBySprintID(id));
    }

    @GetMapping("/details/{id}")
    public ResponseEntity<IssueDTO> getIssueDetailsByID(@PathVariable Integer id) {
        return ResponseEntity.ok(issueService.getIssueDetails(id));
    }

    @PostMapping
    public ResponseEntity<IssueDTO> createIssue(@RequestBody IssueMRO issueDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issueService.createIssue(issueDTO));
    }

    @PutMapping
    public ResponseEntity<IssueDTO> updateIssue(@RequestBody IssueMRO issueDTO) {
        return ResponseEntity.ok(issueService.updateIssue(issueDTO));
    }

    @DeleteMapping("/{id}")
    public void deleteIssue(@PathVariable Integer id){
        issueService.deleteIssue(id);
    }

    @PostMapping("/{id}/assign")
    public void assignCurrentUserToIssue(@PathVariable Integer id) {
        issueService.assignCurrentUserToIssue(id);
    }

    @PutMapping("/{id}/delete-assignment")
    public void deleteUserToIssueAssignment(@PathVariable Integer id) {
        issueService.deleteUserToIssueAssignment(id);
    }

    @PatchMapping("/{id}/status/{statusID}")
    public void changeIssueStatus(@PathVariable Integer id, @PathVariable Integer statusID) {
        issueService.changeIssueStatus(id, statusID);
    }
}
