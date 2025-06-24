package com.cho.polio.application.service;

import com.cho.polio.domain.User;
import com.cho.polio.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserReadService {

    private final UserAsyncService userAsyncService;
    private final UserRepository userRepository;
    private final UserCash userCash;


    public Optional<User> watingAndFindUserByNmae(String name) {
        // max wait: 5초
        long maxWaitMillis = 50000;
        long start = System.currentTimeMillis();

        System.out.println(userCash.getNameInProgress().contains(name));
        while (userCash.getNameInProgress().contains(name)) {
            if (System.currentTimeMillis() - start > maxWaitMillis) {
                //throw new RuntimeException("Timeout waiting for name: " + name);
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted", e);
            }
        }

        // 지금 들어있는 것과 요청한게 달라.
        System.out.println(userCash.getNameInProgress().contains(name));
        return findUserByNmae(name);
    }

    public Optional<User> findUserByNmae(String name) {
        // 리드서비스로 나누면 브레이크 포인트를 걸어도 조회가 된다.
        Optional<User> optionalUser = userRepository.findByName(name);
        optionalUser.ifPresent(user -> {
            System.out.println(user.getName());
        });
        return optionalUser;
    }

    public String createUserName(String mode) {
        String genName = UUID.randomUUID().toString();
        userAsyncService.asyncCall(genName, mode);
        return genName;
    }

    public String changeUserName() {
        String genName = UUID.randomUUID().toString();
        userAsyncService.asyncRequestChangeUserName(genName);
        return genName;
    }


}
