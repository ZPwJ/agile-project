package io.github.student.agile.resource.repository;

import io.github.student.agile.resource.domain.SprintProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SprintProgressRepository extends JpaRepository<SprintProgress, Long> {

    List<SprintProgress> findAllBySprintIdOrderByCapturedAtAscIdAsc(Integer sprintID);
}
