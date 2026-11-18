package io.github.student.agile.resource.repository;

import io.github.student.agile.resource.domain.Comment;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Integer> {

    List<Issue> findAllBySprint(Sprint sprint);

    @Query("SELECT i FROM Issue i LEFT JOIN FETCH i.issueStatus WHERE i.sprint.id = :sprintId")
    List<Issue> findAllBySprintId(@Param("sprintId") Integer sprintId);

    List<Issue> findAllByAssigneeID(String assigneeID);
    Issue findByCommentsContaining(Comment comment);
}
