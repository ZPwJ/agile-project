package io.github.student.agile.resource.repository;

import io.github.student.agile.resource.domain.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueStatusRepository extends JpaRepository<IssueStatus, Integer> {
}
