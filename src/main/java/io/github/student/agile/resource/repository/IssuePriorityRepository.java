package io.github.student.agile.resource.repository;

import io.github.student.agile.resource.domain.IssuePriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IssuePriorityRepository extends JpaRepository<IssuePriority, Integer> {
}
