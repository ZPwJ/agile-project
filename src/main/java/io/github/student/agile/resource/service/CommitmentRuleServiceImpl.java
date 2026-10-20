package io.github.student.agile.resource.service;

import io.github.student.agile.resource.domain.CommitmentRule;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.dto.CommitmentRuleDTO;
import io.github.student.agile.resource.mapper.CommitmentRuleDTOMapper;
import io.github.student.agile.resource.repository.CommitmentRuleRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;

@Service
@RequiredArgsConstructor
public class CommitmentRuleServiceImpl implements CommitmentRuleService {

    private final CommitmentRuleRepository ruleRepository;
    private final ProjectRepository projectRepository;
    private final CommitmentRuleDTOMapper ruleDTOMapper;

    @Override
    public Set<CommitmentRuleDTO> getAllRules(Integer projectID) {

        Project project = projectRepository.findById(projectID).orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));
        return project.getDod().stream()
                .map(ruleDTOMapper::map)
                .collect(Collectors.toSet());
    }

    @Override
    public CommitmentRuleDTO defineRule(Integer projectID, String rule) {

        Project project = projectRepository.findById(projectID).orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));
        CommitmentRule singleRule = new CommitmentRule();
        singleRule.setRule(rule);
        ruleRepository.save(singleRule);

        Set<CommitmentRule> rules = project.getDod();
        rules.add(singleRule);
        project.setDod(rules);
        projectRepository.save(project);

        return ruleDTOMapper.map(singleRule);
    }

    @Override
    public CommitmentRuleDTO updateRule(Integer ruleID, String rule) {

        CommitmentRule singleRule = ruleRepository.findById(ruleID).orElseThrow();
        singleRule.setRule(rule);
        ruleRepository.save(singleRule);
        return ruleDTOMapper.map(singleRule);
    }

    @Override
    public void deleteRule(Integer projectID, Integer ruleID) {

        Project project = projectRepository.findById(projectID).orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));
        CommitmentRule ruleToDelete = ruleRepository.findById(ruleID).orElseThrow();
        Set<CommitmentRule> projectRules = project.getDod();
        projectRules.remove(ruleToDelete);
        project.setDod(projectRules);
        projectRepository.save(project);
        ruleRepository.deleteById(ruleID);
    }

    @Override
    public void deleteAllRules(Integer projectID) {

        Project project = projectRepository.findById(projectID).orElseThrow(() -> new RuntimeException(PROJECT_DOES_NOT_EXISTS));
        Set<CommitmentRule> projectRules = project.getDod();
        project.setDod(new HashSet<>());
        projectRepository.save(project);

        projectRules.forEach(rule -> ruleRepository.deleteById(rule.getId()));
    }
}
