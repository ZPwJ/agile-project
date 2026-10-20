package io.github.student.agile.resource.service;

import io.github.student.agile.resource.dto.IssueDTO;
import io.github.student.agile.resource.dto.IssueGeneralDTO;
import io.github.student.agile.resource.dto.request.IssueMRO;

import java.util.List;

public interface IssueService {

    IssueDTO createIssue(IssueMRO issue);
    IssueDTO updateIssue(IssueMRO issue);
    void deleteIssue(Integer issueID);
    IssueDTO getIssueDetails(Integer issueID);
    List<IssueGeneralDTO> getIssuesGeneral(Integer projectID);
    List<IssueGeneralDTO> getProductBacklog(Integer projectID);
    List<IssueGeneralDTO> getBacklogHistory(Integer projectID);
    List<IssueGeneralDTO> getGeneralIssuesInfoByUserID();
    List<IssueGeneralDTO> getGeneralIssuesInfoBySprintID(Integer sprintID);
    void assignCurrentUserToIssue(Integer issueID);
    void deleteUserToIssueAssignment(Integer issueID);
    void changeIssueStatus(Integer issueID, Integer statusID);
}
