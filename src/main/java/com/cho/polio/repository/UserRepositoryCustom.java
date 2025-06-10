package com.cho.polio.repository;

import com.cho.polio.domain.User;

import java.util.List;

public interface UserRepositoryCustom {
    List<User> findByName(String name);
}
