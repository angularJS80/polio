package com.cho.polio.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserSchduleComponent {
    private final UserService userService;

    public void requestUpdateUserName(){
        userService.findAll().stream()
                .forEach(user ->  {
                    userService.changeNextName(user);
                });
    }
}
