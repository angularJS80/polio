package com.cho.polio.application.service;

import com.cho.polio.domain.User;
import com.cho.polio.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findUserByNmae(String name) {

        return userRepository.findByName(name);

    }

    public void regist(){
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/user/create-user-name";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        String name = response.getBody(); // 실제 응답 본문만 추출
        userRepository.save(new User(name));
    }

    public String createUserName() {
        String genName = UUID.randomUUID().toString();
        RestTemplate restTemplate = new RestTemplate();
        String registUrl = "http://localhost:8080/user/async-regist-test?name="+genName;
        // 결과 무시
        restTemplate.getForEntity(registUrl, Void.class);

        String isRegistUrl = "http://localhost:8080/user/async-test?name="+genName;
        // 결과 무시
        restTemplate.getForEntity(isRegistUrl, Void.class);


        return UUID.randomUUID().toString();
    }
}
