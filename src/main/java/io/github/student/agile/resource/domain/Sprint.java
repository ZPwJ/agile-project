package io.github.student.agile.resource.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Sprint extends Auditable implements Serializable {

    @Id
    @Column(name = "SprintID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sprint_seq_gen")
    @SequenceGenerator(name = "sprint_seq_gen", sequenceName = "sprint_seq")
    private Integer id;

    @NotNull
    private String name;

    private ZonedDateTime startDate;

    @ManyToOne
    @JoinColumn(name = "ProjectID", nullable = false)
    private Project project;

    @OneToMany(mappedBy = "sprint")
    private Set<Issue> sprintIssues;

    private Boolean active;

    private String sprintGoal;

    private Boolean isDone;
}
