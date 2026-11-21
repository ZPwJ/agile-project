
package io.github.student.agile.resource.analytics.reports;

import io.github.student.agile.resource.domain.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SprintComparisonRepository
        extends JpaRepository<Sprint, Integer> {

    List<Sprint> findByProjectId(Integer projectId);
}
