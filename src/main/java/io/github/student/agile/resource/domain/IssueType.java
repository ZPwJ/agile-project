package io.github.student.agile.resource.domain;

import io.github.student.agile.resource.domain.dictionary.IssueTypeEnum;
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
public class IssueType {

    @Id
    @Column(name = "Issue_TypeID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "issue_type_seq_gen")
    @SequenceGenerator(name = "issue_type_seq_gen", sequenceName = "issue_type_seq")
    private Integer id;

    @Enumerated(EnumType.STRING)
    private IssueTypeEnum type;
}
