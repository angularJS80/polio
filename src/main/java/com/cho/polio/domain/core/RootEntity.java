package com.cho.polio.domain.core;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 최상위 루트 엔터티
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class RootEntity extends DomainEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @CreatedBy
    @Column(name = "create_id", nullable = false, updatable = false)
    protected String createId;

    @CreatedDate
    @Column(name = "create_dt", nullable = false, updatable = false)
    protected LocalDateTime createDt;

    @LastModifiedBy
    @Column(name = "update_id", nullable = false)
    protected String updateId;

    @LastModifiedDate
    @Column(name = "update_dt", nullable = false)
    protected LocalDateTime updateDt;

    public LocalDateTime getCreateDt() {
        return null == createDt ? createDt : createDt.atZone(ZoneId.systemDefault())
            .withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime();
    }

    public LocalDateTime getUpdateDt() {
        return null == updateDt ? updateDt : updateDt.atZone(ZoneId.systemDefault())
            .withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime();
    }
}
