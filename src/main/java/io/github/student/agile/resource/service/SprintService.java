package io.github.student.agile.resource.service;

import io.github.student.agile.resource.dto.SprintDTO;

import java.util.List;

public interface SprintService {

    SprintDTO getActiveSprint(Integer projectID);
    List<SprintDTO> getAllNonActivatedSprints(Integer projectID);
    SprintDTO createSprint(SprintDTO sprintDTO);
    SprintDTO updateSprint(SprintDTO sprintDTO);
    void deleteSprint(Integer sprintID);
    void addIssueToSprint(Integer issueID, Integer sprintID);
    void deleteIssueFromSprint(Integer sprintID, Integer issueID);
    void startSprint(Integer sprintID);
    void stopSprint(Integer projectID);
}
