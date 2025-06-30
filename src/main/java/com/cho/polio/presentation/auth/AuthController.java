package com.cho.polio.presentation.auth;

import com.cho.polio.presentation.auth.dto.RequestPassword;
import com.cho.polio.presentation.auth.dto.RequestRefresh;
import com.cho.polio.presentation.auth.dto.RequestSendReset;
import com.cho.polio.presentation.enums.ApiPaths;
import com.polio.poliokeycloak.keycloak.helper.KeycloakAuthHelper;
import com.polio.poliokeycloak.keycloak.helper.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.AUTH)
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakAuthHelper keycloakAuthHelper;

    @Autowired
    private JavaMailSender mailSender;

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> signIn(@RequestBody UserLoginRequest userLoginRequest) {

        UserLoginResponse userLoginResponse = keycloakAuthHelper.signIn(userLoginRequest);

        return ResponseEntity.ok(userLoginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserLoginResponse> refresh(@RequestBody RequestRefresh refreshToken) {

        UserLoginResponse userLoginResponse = keycloakAuthHelper.refresh(refreshToken.getRefreshToken());

        return ResponseEntity.ok(userLoginResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> signOut(@RequestBody RequestRefresh refreshToken) {

        keycloakAuthHelper.signOut(refreshToken.getRefreshToken());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/join")
    public ResponseEntity<Void> join(@RequestBody UserRegisterRequest userRegisterRequest) {
        keycloakAuthHelper.regist(userRegisterRequest);
        return ResponseEntity.ok().build();
    }


    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestBody RequestPassword requestPassword,
                                               @AuthenticationPrincipal Jwt jwt) {
        keycloakAuthHelper.changeUserPassword(new UserChangePasswordRequest(jwt.getSubject(), requestPassword.getNewPassword()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-reset")
    public ResponseEntity<Void> sendResetPassword(@RequestBody RequestSendReset requestSendReset) {

        String id =  keycloakAuthHelper.findUserByEmail(requestSendReset.getEmail());
        String accessToken = keycloakAuthHelper.tokenExchangeAsUser(
                new ExchangeUserRequest(id, "polio-toy-client-action","password-reset")
                ).accessToken();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(requestSendReset.getEmail());
        message.setSubject("테스트 메일");
        message.setText("http://localhost:3000/reset-password?access_token="+accessToken);

        mailSender.send(message);

        return ResponseEntity.ok().build();
    }



    @PutMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody RequestPassword requestPassword,
                                               @AuthenticationPrincipal Jwt jwt) {

        keycloakAuthHelper.changeUserPassword(new UserChangePasswordRequest(jwt.getSubject(), requestPassword.getNewPassword()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Jwt> getMyInfo(@AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(jwt);
    }


}