package io.github.student.agile.resource.repository;

import io.github.student.agile.resource.domain.CommitmentRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommitmentRuleRepository extends JpaRepository<CommitmentRule, Integer> {

}
