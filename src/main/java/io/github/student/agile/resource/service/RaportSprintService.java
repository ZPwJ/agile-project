package io.github.student.agile.resource.service;


import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.dto.SprintDTO;
import io.github.student.agile.resource.dto.SprintRaportDTO;
import io.github.student.agile.resource.mapper.SprintDTOMapper;
import io.github.student.agile.resource.mapper.SprintRaportDTOMapper;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.IssueStatusRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
@RequiredArgsConstructor
public class RaportSprintService {

    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final SprintRaportDTOMapper sprintraportDTOMapper;

    private Integer countIssuesBySprintId(Integer sprintId){
        return issueRepository.countIssuesBySprint_Id(sprintId);
    }

    private Integer countIssuesDoneBySprintId(Integer sprintId){
        return issueRepository.countIssuesDoneBySprint_Id(sprintId);
    }

    private Integer countIssuesNotDoneBySprintId(Integer sprintId){
        return countIssuesBySprintId(sprintId) - countIssuesDoneBySprintId(sprintId);
    }

    private Integer sumVelocityIssuesDoneBySprint_Id(Integer sprintId){
        return issueRepository.sumVelocityIssuesDoneBySprint_Id(sprintId);
    }

    private Integer sumVelocityIssuesNotDoneBySprint_Id(Integer sprintId){
        return issueRepository.sumVelocityIssuesNotDoneBySprint_Id(sprintId);
    }

    public SprintRaportDTO getRaport(Integer sprintId){
        Sprint sprint = sprintRepository.findSprintById(sprintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint not found"));

        if(sprint != null){
            return sprintraportDTOMapper.map(
                    sprintRepository.findSprintById(sprintId).get(),
                    countIssuesBySprintId(sprintId),
                    countIssuesDoneBySprintId(sprintId),
                    countIssuesNotDoneBySprintId(sprintId),
                    sumVelocityIssuesDoneBySprint_Id(sprintId),
                    sumVelocityIssuesNotDoneBySprint_Id(sprintId)
            );

        }
        return null;

    }
}
