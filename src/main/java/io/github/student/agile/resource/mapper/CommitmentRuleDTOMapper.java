package io.github.student.agile.resource.mapper;

import io.github.student.agile.resource.domain.CommitmentRule;
import io.github.student.agile.resource.dto.CommitmentRuleDTO;
import org.springframework.stereotype.Component;

@Component
public class CommitmentRuleDTOMapper {

    public CommitmentRuleDTO map(CommitmentRule from) {
        CommitmentRuleDTO rule = new CommitmentRuleDTO();
        rule.setRuleID(from.getId());
        rule.setRuleName(from.getRule());
        return rule;
    }
}
