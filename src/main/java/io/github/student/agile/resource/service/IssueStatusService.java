package io.github.student.agile.resource.service;

import io.github.student.agile.resource.domain.IssueStatus;

import java.util.List;

public interface IssueStatusService {

    List<IssueStatus> getAllIssueStatuses();
}
