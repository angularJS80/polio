package com.cho.polio.domain.core;

import java.io.Serializable;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * 최상위 도메인 엔터티
 */
@Getter
@MappedSuperclass
public abstract class DomainEntity implements Serializable {

}
