package com.cho.polio.presentation.user;

import com.cho.polio.application.service.UserService;
import com.cho.polio.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/async-test") // 이름만 async-test일 뿐 동기로 동작
    public ResponseEntity<?> getUser(@RequestParam String name) {
        Optional<User> userOptional = userService.findUserByNmae(name);

        if (userOptional.isPresent()) {
            return ResponseEntity.ok(userOptional.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/async-regist-test") // 이름만 async-test일 뿐 동기로 동작
    public ResponseEntity<?> saveUser() {
         userService.regist();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/create-user-name") // 이름만 async-test일 뿐 동기로 동작
    public ResponseEntity<String> crateUserName() {

        return ResponseEntity.ok(userService.createUserName());
    }
}
