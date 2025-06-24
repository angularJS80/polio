package com.cho.polio.repository;

import com.cho.polio.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {


    Optional<User> findByName(String name);

    Optional<User> findByNextName(String nextName);
}
