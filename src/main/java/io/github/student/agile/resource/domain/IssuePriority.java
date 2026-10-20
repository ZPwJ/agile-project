package io.github.student.agile.resource.domain;

import io.github.student.agile.resource.domain.dictionary.IssuePriorityEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class IssuePriority {

    @Id
    @Column(name = "Issue_PriorityID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "issue_priority_seq_gen")
    @SequenceGenerator(name = "issue_priority_seq_gen", sequenceName = "issue_priority_seq")
    private Integer id;

    @Enumerated(EnumType.STRING)
    private IssuePriorityEnum priority;
}
