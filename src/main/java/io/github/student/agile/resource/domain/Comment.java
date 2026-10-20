package io.github.student.agile.resource.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@Entity
@Table(name = "comments")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Comment extends Auditable implements Serializable {

    @Id
    @Column(name = "CommentID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comments_seq_gen")
    @SequenceGenerator(name = "comments_seq_gen", sequenceName = "comments_seq")
    private Integer id;

    @NotNull
    private String body;
}

