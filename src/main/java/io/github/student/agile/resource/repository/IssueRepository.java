package io.github.student.agile.resource.repository;

import io.github.student.agile.resource.domain.Comment;
import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Integer> {

    List<Issue> findAllBySprint(Sprint sprint);
    List<Issue> findAllByAssigneeID(String assigneeID);
    Issue findByCommentsContaining(Comment comment);

    Integer countIssuesBySprint_Id(Integer sprintId);

    @Query("""
        SELECT COUNT(i)
        FROM Issue i
        JOIN i.issueStatus s
        WHERE i.sprint.id = :sprintId
        AND s.statusName = 'DONE'
    """)
    Integer countIssuesDoneBySprint_Id(Integer sprintId);

    @Query("""
        SELECT SUM(i.estimation)
        FROM Issue i
        JOIN i.issueStatus s
        WHERE i.sprint.id = :sprintId
        AND s.statusName = 'DONE'
    """)
    Integer sumVelocityIssuesDoneBySprint_Id(Integer sprintId);


    @Query("""
        SELECT SUM(i.estimation)
        FROM Issue i
        JOIN i.issueStatus s
        WHERE i.sprint.id = :sprintId
        AND s.statusName != 'DONE'
    """)
    Integer sumVelocityIssuesNotDoneBySprint_Id(Integer sprintId);

}
