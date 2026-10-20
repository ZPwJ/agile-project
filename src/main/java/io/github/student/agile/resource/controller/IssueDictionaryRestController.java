package io.github.student.agile.resource.controller;

import io.github.student.agile.resource.domain.IssuePriority;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.IssueType;
import io.github.student.agile.resource.service.IssuePriorityService;
import io.github.student.agile.resource.service.IssueStatusService;
import io.github.student.agile.resource.service.IssueTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dictionary")
@RequiredArgsConstructor
public class IssueDictionaryRestController {

    private final IssueTypeService issueTypeService;
    private final IssueStatusService issueStatusService;
    private final IssuePriorityService issuePriorityService;

    @GetMapping("/type")
    public List<IssueType> getAllIssueTypes() {
        return issueTypeService.getAllIssueTypes();
    }

    @GetMapping("/status")
    public List<IssueStatus> getAllIssueStatuses() {
        return issueStatusService.getAllIssueStatuses();
    }

    @GetMapping("/priority")
    public List<IssuePriority> getAllIssuePriorities() {
        return issuePriorityService.getAllIssuePriorities();
    }
}
