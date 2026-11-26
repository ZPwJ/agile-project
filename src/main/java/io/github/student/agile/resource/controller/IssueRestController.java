package io.github.student.agile.resource.controller;

import io.github.student.agile.resource.dto.IssueDTO;
import io.github.student.agile.resource.dto.IssueGeneralDTO;
import io.github.student.agile.resource.dto.request.IssueMRO;
import io.github.student.agile.resource.service.IssueService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "Gets all issues in the backlog with the status TO_DO and no assigned sprint.",
            description = "Does not return the whole backlog. For completed issues, use /product-backlog-history. For issues included in a sprint backlog, use /sprint/{id}.")
    @GetMapping("/product-backlog")
    public ResponseEntity<List<IssueGeneralDTO>> getProductBacklogByProjectID(@RequestParam Integer projectID) {
        return ResponseEntity.ok(issueService.getProductBacklog(projectID));
    }

    @Operation(summary = "Gets all issues in the backlog with the status DONE.")
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

    @Operation(summary = "Assigns the issue to the current user")
    @PostMapping("/{id}/assign")
    public void assignCurrentUserToIssue(@PathVariable Integer id) {
        issueService.assignCurrentUserToIssue(id);
    }

    @Operation(summary = "Removes the current user assignment",description = "The user doesn't have to be the current one.")
    @PutMapping("/{id}/delete-assignment")
    public void deleteUserToIssueAssignment(@PathVariable Integer id) {
        issueService.deleteUserToIssueAssignment(id);
    }

    @Operation(summary = "Changes the status",description = "Allowed: TO_DO->IN_PROGRESS, IN_PROGRESS->CODE_REVIEW, CODE_REVIEW->IN_TESTS, IN_TESTS->DONE, CODE_REVIEW->IN_PROGRESS, IN_TESTS->IN_PROGRESS")
    @PatchMapping("/{id}/status/{statusID}")
    public void changeIssueStatus(@PathVariable Integer id, @PathVariable Integer statusID) {
        issueService.changeIssueStatus(id, statusID);
    }
}
