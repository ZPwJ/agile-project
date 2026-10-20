package io.github.student.agile.resource.repository;

import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Integer> {

    Optional<List<Sprint>> findAllByActiveAndIsDoneAndProject(Boolean active, Boolean isDone ,Project project);
    Optional<Sprint> findByActiveAndProject(Boolean active, Project project);
    boolean existsByNameAndProject(String name, Project project);
}
