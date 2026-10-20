package io.github.student.agile.resource.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedBy
    @Column
    protected String createdBy;

    @CreatedDate
    @Column
    protected Date createdDate;

    @LastModifiedBy
    @Column
    protected String lastModifiedBy;

    @LastModifiedDate
    @Column
    protected Date lastModifiedDate;
}
