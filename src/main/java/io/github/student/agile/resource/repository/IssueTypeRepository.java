package io.github.student.agile.resource.repository;

import io.github.student.agile.resource.domain.IssueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueTypeRepository extends JpaRepository<IssueType, Integer> {
}
