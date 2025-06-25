package com.cho.polio.application.service;

import com.cho.polio.domain.User;
import com.cho.polio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserAsyncService {

    private final RestTemplate restTemplate;

    @Async("httpSend")
    public void asyncCall(String genName,String mode){

        String findUrl = "http://localhost:8080/user/find?name="+genName+"&mode="+mode;
        System.out.println(findUrl);
        // 결과 무시
        try {
            restTemplate.getForEntity(findUrl, Void.class);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

    }


    @Async("httpSend")
    public void asyncRequestChangeUserName(String genName){

       
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
