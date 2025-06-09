package com.cho.polio.repository;

import com.cho.polio.domain.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.List;

import static com.cho.polio.domain.QUser.user;

@Repository
public class UserRepositoryImpl implements UserRepositoryCustom {


    private final JPAQueryFactory queryFactory;

    public UserRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<User> findByName(String name) {
        return queryFactory.selectFrom(user)
                .where(user.name.eq(name))
                .fetch();
    }
}
