package io.github.student.agile.resource.repository;

import io.github.student.agile.resource.domain.Backlog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacklogRepository extends JpaRepository<Backlog, Integer> {
}
