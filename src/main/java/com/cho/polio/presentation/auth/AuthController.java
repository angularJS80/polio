package com.cho.polio.presentation.auth;

import com.cho.polio.presentation.auth.dto.*;
import com.cho.polio.presentation.enums.ApiPaths;
import com.polio.poliokeycloak.keycloak.helper.KeycloakAuthHelper;
import com.polio.poliokeycloak.keycloak.helper.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

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


    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody RequestPassword requestPassword,
                                               @AuthenticationPrincipal Jwt jwt) {
        keycloakAuthHelper.changeUserPassword(new UserChangePasswordRequest(jwt.getSubject(), requestPassword.getNewPassword()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/find-password")
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


    @GetMapping("/social-login")
    public ResponseEntity<?> proxyToKeycloak(
            @RequestParam String idp
            ,@RequestParam String scope
            ,@RequestParam String redirectUrl

    ) {
        String keycloakUrl =  keycloakAuthHelper.getOauthIdpLoginLink(new OauthLinkRequest(
                idp
                ,redirectUrl
                , scope
        ));
        URI uri = URI.create(keycloakUrl);  //

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uri);
        return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 Redirect
    }


    @PostMapping("/login-by-code")
    public ResponseEntity<UserLoginResponse> loginByCodePost(@RequestBody RequestCode requestCode) {

        CodeLoginRequest request = new CodeLoginRequest(requestCode.getCode(), "http://localhost:3000/auth/callback");
        UserLoginResponse response = keycloakAuthHelper.refreshByCode(request);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/login-by-code")
    public ResponseEntity<UserLoginResponse> loginByCodeRedirect(@RequestParam String code,@RequestParam(name = "redirect_uri", required = false) String redirectUri) {
        CodeLoginRequest request = new CodeLoginRequest(code, "http://localhost:8080/auth/callback");
        UserLoginResponse response = keycloakAuthHelper.refreshByCode(request);
         return ResponseEntity.ok(response);
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