package com.cho.polio;

import com.cho.polio.config.AuditorAwareImpl;
import com.cho.polio.domain.User;
import com.cho.polio.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@EnableJpaAuditing  // <- 이걸 추가해야 Auditing이 활성화됨
@Import(AuditorAwareImpl.class) // AuditorAware 빈도 함께 등록 (필요시)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Test
    void 사용자_저장_및_조회() {
        User saved = userRepository.save(new User("cho"));
        List<User> dsluser = userRepository.findByName("cho");

        Optional<User> found = userRepository.findById(saved.getId());


        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("cho");
    }
}
