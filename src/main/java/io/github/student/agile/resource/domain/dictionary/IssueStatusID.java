package io.github.student.agile.resource.domain.dictionary;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IssueStatusID {
    TO_DO(1),
    IN_PROGRESS(2),
    CODE_REVIEW(3),
    IN_TESTS(4),
    DONE(5);

    private final Integer statusID;
}
