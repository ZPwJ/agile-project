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
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class RaportSprintService {

    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final SprintRaportDTOMapper sprintraportDTOMapper;
    private final IssueStatusRepository issueStatusRepository;

    private Integer countIssuesBySprintId(Integer sprintId){
        return issueRepository.countIssuesBySprint_Id(sprintId);
    }

    private Integer countIssuesDoneBySprintId(Integer sprintId){
        return issueStatusRepository.countByIdAndStatusName_Done(sprintId);
    }

    public SprintRaportDTO getRaport(Integer sprintId){
        //zamienic mapper
        Sprint sprint = sprintRepository.findSprintById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found"));

        if(sprintRepository.findSprintById(sprintId) != null){
            return sprintraportDTOMapper.map(sprintRepository.findSprintById(sprintId).get(), countIssuesBySprintId(sprintId));
        }
        return null;

    }
}
