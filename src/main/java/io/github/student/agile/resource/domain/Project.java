package io.github.student.agile.resource.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Project extends Auditable implements Serializable {

    @Id
    @Column(name = "ProjectID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_seq_gen")
    @SequenceGenerator(name = "project_seq_gen", sequenceName = "project_seq")
    private Integer id;

    @NotNull
    private String name;

    private String description;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "BacklogID", referencedColumnName = "BacklogID")
    private Backlog backlog;

    @Builder.Default
    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "users", joinColumns = @JoinColumn(name = "project_id"))
    private Set<String> userIDs = new HashSet<>();

    private String projectOwner;

    @Builder.Default
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private Set<Sprint> sprints = new HashSet<>();

    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL)
    private Set<CommitmentRule> dod = new HashSet<>();

    private String projectGoal;
}
