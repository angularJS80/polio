package com.cho.polio.presentation.user;

import com.cho.polio.application.service.UserReadService;
import com.cho.polio.application.service.UserService;
import com.cho.polio.application.service.UserServiceWithOutTransaction;
import com.cho.polio.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserReadService userReadService;
    private final UserServiceWithOutTransaction userServiceWithOutTransaction;

    @GetMapping("/find") // 이름만 async-test일 뿐 동기로 동작
    public ResponseEntity<?> getDefaultServiceUser(  @RequestParam String name, @RequestParam String mode) {
        Optional<User> userOptional = Optional.empty();
        if(mode.equals("single-service")){
             userOptional = userService.watingAndFindUserByNmae(name);
        }else{
            userOptional = userReadService.watingAndFindUserByNmae(name);
        }

        if (userOptional.isPresent()) {
            return ResponseEntity.ok(userOptional.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/regist") // 이름만 async-test일 뿐 동기로 동작
    public ResponseEntity<?> saveUser(@RequestParam String mode) {
         userService.regist(mode);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/change") // 이름만 async-test일 뿐 동기로 동작
    public ResponseEntity<?> changeUser(@RequestParam String nextName) {
        //userServiceWithOutTransaction.change(nextName);
        userService.watingAndChange(nextName);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/create-user-name") // 이름만 async-test일 뿐 동기로 동작
    public ResponseEntity<String> crateUserName(@RequestParam String mode) {

        return ResponseEntity.ok(userReadService.createUserName(mode));
    }

    @GetMapping("/change-user-name") // 이름만 async-test일 뿐 동기로 동작
    public ResponseEntity<String> changeUserName() {

        return ResponseEntity.ok(userReadService.changeUserName());
    }

}
