package com.cho.polio.presentation.auth;

import com.cho.polio.presentation.auth.dto.RequestRefresh;
import com.cho.polio.presentation.enums.ApiPaths;
import com.polio.poliokeycloak.keycloak.helper.KeycloakAuthHelper;
import com.polio.poliokeycloak.keycloak.helper.dto.UserChangePasswordRequest;
import com.polio.poliokeycloak.keycloak.helper.dto.UserLoginRequest;
import com.polio.poliokeycloak.keycloak.helper.dto.UserLoginResponse;
import com.polio.poliokeycloak.keycloak.helper.dto.UserRegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.AUTH)
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakAuthHelper keycloakAuthHelper;

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
    public ResponseEntity<Void> changePassword(@RequestBody UserChangePasswordRequest userChangePasswordRequest) {
        keycloakAuthHelper.changeUserPassword(userChangePasswordRequest);
        return ResponseEntity.ok().build();
    }

}