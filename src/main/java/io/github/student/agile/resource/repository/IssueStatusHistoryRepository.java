package io.github.student.agile.resource.repository;

import io.github.student.agile.resource.domain.IssueStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueStatusHistoryRepository extends JpaRepository<IssueStatusHistory, Integer> {

    List<IssueStatusHistory> findAllByIssueIdOrderByChangedAtAsc(Integer issueId);

    List<IssueStatusHistory> findAllBySprintIdOrderByChangedAtAsc(Integer sprintId);
}
