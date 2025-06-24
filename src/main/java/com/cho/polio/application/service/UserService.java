package com.cho.polio.application.service;

import com.cho.polio.domain.User;
import com.cho.polio.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserAsyncService userAsyncService;
    private final UserRepository userRepository;
    private final UserCash userCash;

    public void regist(String mode){
        //Break Point (첫번째 트랜잭션 시작상태를 감지 )
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/user/create-user-name?mode="+mode;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        String name = response.getBody(); // 실제 응답 본문만 추출
        userCash.getNameInProgress().add(name);
        userRepository.save(new User(name));

        // 트랜잭션 커밋 후 실행
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted", e);
                }
                userCash.getNameInProgress().remove(name);
                System.out.println("name removed after commit");
            }
        });

    }

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

        System.out.println(userCash.getNameInProgress().contains(name));
        return findUserByNmae(name);
    }

    public Optional<User> findUserByNmae(String name) {
        //Break Point (두번째 트랜잭션 시작상태를 머무르게 하여 등록에서 걸어논 트랜젝션과, 조회에서 걸어논 트랜젝션이 동시에 머물게)
        Optional<User> optionalUser = userRepository.findByName(name);
        optionalUser.ifPresent(user -> {
            // 두 브래이크 포인트를 걸면 조회가 안된다.
            System.out.println(user.getName());
        });
        return optionalUser;
    }


    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Async
    public void change(String nextName) {

        // max wait: 5초
        long maxWaitMillis = 50000;
        long start = System.currentTimeMillis();

        while (userCash.getNextNameInProgress().contains(nextName)) {
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

        userRepository.findByNextName(nextName)
                .ifPresent(user -> {

                    user.updateNameFromNextName();
                    userRepository.save(user);
                });

    }

    public void save(User user) {
        userRepository.save(user);
    }


    @Async
    public void changeNextName(User user){
        String nextName = requestUserName();
        user.updateNextName(requestUserName());
        save(user);

        // 트랜잭션 커밋 후 실행
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted", e);
                }
                userCash.getNameInProgress().remove(nextName);
                System.out.println("name removed after commit");
            }
        });

    }


    public String requestUserName(){
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/user/change-user-name";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        String nextName = response.getBody();
        userCash.getNextNameInProgress().add(nextName);
        return nextName;
    }

}
