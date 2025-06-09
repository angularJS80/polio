package com.cho.polio.domain;

import com.cho.polio.domain.core.RootEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "`user`")
public class User extends RootEntity {


    private String name;

    protected User() {} // JPA용 기본 생성자

    public User(String name) {
        this.name = name;
    }

    // getter 생략 가능 (테스트용이라면 Lombok 사용해도 무방)
    public String getName() {
        return name;
    }

}