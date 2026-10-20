package io.github.student.agile.resource.domain;

import jakarta.persistence.Entity;
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
public class CommitmentRule {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dod_rule_seq_gen")
    @SequenceGenerator(name = "dod_rule_seq_gen", sequenceName = "dod_rule_seq")
    private Integer id;

    private String rule;
}
