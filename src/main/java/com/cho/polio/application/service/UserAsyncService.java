package com.cho.polio.application.service;

import com.cho.polio.domain.User;
import com.cho.polio.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserAsyncService {

    @Async
    public void asyncCall(String genName,String mode){

        RestTemplate restTemplate = new RestTemplate();
        String findUrl = "http://localhost:8080/user/find?name="+genName+"&mode="+mode;
        System.out.println(findUrl);
        // 결과 무시
        try {
            restTemplate.getForEntity(findUrl, Void.class);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

    }


    @Async
    public void asyncRequestChangeUserName(String genName){

        RestTemplate restTemplate = new RestTemplate();
        String findUrl = "http://localhost:8080/user/change?nextName="+genName;
        System.out.println(findUrl);
        // 결과 무시
        try {
            restTemplate.getForEntity(findUrl, Void.class);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

    }

}
